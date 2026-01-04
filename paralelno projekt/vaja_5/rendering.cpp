#define WIN32_LEAN_AND_MEAN
#include <WinSock2.h>
#include <ws2tcpip.h>
#include "rendering.h"
#include "blockchain.h"
#include "networking.h"
#include "block.h"
#include <format>
#include <algorithm>
#include <functional>
#include <ctime>
#include <chrono>

namespace rendering {

    static ID3D11Device* g_pd3dDevice = nullptr;
    static ID3D11DeviceContext* g_pd3dDeviceContext = nullptr;
    static IDXGISwapChain* g_pSwapChain = nullptr;
    static bool g_SwapChainOccluded = false;
    static UINT g_ResizeWidth = 0, g_ResizeHeight = 0;
    static ID3D11RenderTargetView* g_mainRenderTargetView = nullptr;

    static HWND window_hwnd = NULL;
    static WNDCLASSEXW wc;
    static bool initialized = false;

    bool done = false;
    ImGuiWindowFlags window_flags = ImGuiWindowFlags_NoTitleBar | ImGuiWindowFlags_NoMove | ImGuiWindowFlags_NoResize;

    std::mutex log_list_mtx;
    std::vector<log_msg> log_list;
    bool scroll_to_bottom = false;

    log_msg::log_msg(const std::string& m, const std::initializer_list<uint8_t> c) : data(m), label("LOG"), show_label(true) {
        color = *(uint32_t*)c.begin();
    }

    log_msg::log_msg(const std::string& lbl, const std::string& m, const std::initializer_list<uint8_t> c, bool s) : label(lbl), data(m), show_label(s) {
        color = *(uint32_t*)c.begin();
    }

    // Retrieves the DPI scale factor for a window.
    float get_window_content_scale(HWND window) {
        int dpi = GetDpiForWindow(window);
        float dpi_scale = dpi / 96.f;
        return dpi_scale;
    }

    // Creates a Direct3D render target view from the swap chain.
    void create_render_target() {
        ID3D11Texture2D* pBackBuffer;
        g_pSwapChain->GetBuffer(0, IID_PPV_ARGS(&pBackBuffer));

        IM_ASSERT(pBackBuffer != nullptr);

        g_pd3dDevice->CreateRenderTargetView(pBackBuffer, nullptr, &g_mainRenderTargetView);
        pBackBuffer->Release();
    }

    // Releases the Direct3D render target view.
    void cleanup_render_target() {
        if (g_mainRenderTargetView) { g_mainRenderTargetView->Release(); g_mainRenderTargetView = nullptr; }
    }

    // Creates the Direct3D device and swap chain for rendering.
    bool create_device_D3D(HWND hWnd) {
        DXGI_SWAP_CHAIN_DESC sd;
        ZeroMemory(&sd, sizeof(sd));
        sd.BufferCount = 2;
        sd.BufferDesc.Width = 0;
        sd.BufferDesc.Height = 0;
        sd.BufferDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
        sd.BufferDesc.RefreshRate.Numerator = 60;
        sd.BufferDesc.RefreshRate.Denominator = 1;
        sd.Flags = DXGI_SWAP_CHAIN_FLAG_ALLOW_MODE_SWITCH;
        sd.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
        sd.OutputWindow = hWnd;
        sd.SampleDesc.Count = 1;
        sd.SampleDesc.Quality = 0;
        sd.Windowed = TRUE;
        sd.SwapEffect = DXGI_SWAP_EFFECT_DISCARD;

        UINT createDeviceFlags = 0;
        D3D_FEATURE_LEVEL featureLevel;
        const D3D_FEATURE_LEVEL featureLevelArray[2] = { D3D_FEATURE_LEVEL_11_0, D3D_FEATURE_LEVEL_10_0, };
        HRESULT res = D3D11CreateDeviceAndSwapChain(nullptr, D3D_DRIVER_TYPE_HARDWARE, nullptr, createDeviceFlags, featureLevelArray, 2, D3D11_SDK_VERSION, &sd, &g_pSwapChain, &g_pd3dDevice, &featureLevel, &g_pd3dDeviceContext);
        if (res == DXGI_ERROR_UNSUPPORTED)
            res = D3D11CreateDeviceAndSwapChain(nullptr, D3D_DRIVER_TYPE_WARP, nullptr, createDeviceFlags, featureLevelArray, 2, D3D11_SDK_VERSION, &sd, &g_pSwapChain, &g_pd3dDevice, &featureLevel, &g_pd3dDeviceContext);
        if (res != S_OK)
            return false;

        create_render_target();
        return true;
    }

    // Releases all Direct3D resources.
    void cleanup_device_D3D() {
        cleanup_render_target();
        if (g_pSwapChain) { g_pSwapChain->Release(); g_pSwapChain = nullptr; }
        if (g_pd3dDeviceContext) { g_pd3dDeviceContext->Release(); g_pd3dDeviceContext = nullptr; }
        if (g_pd3dDevice) { g_pd3dDevice->Release(); g_pd3dDevice = nullptr; }
    }

    // Window message handler for Win32 events.
    LRESULT WINAPI wnd_proc(HWND hWnd, UINT msg, WPARAM wParam, LPARAM lParam) {
        if (ImGui_ImplWin32_WndProcHandler(hWnd, msg, wParam, lParam)) {
            return true;
        }

        switch (msg) {
            case WM_SIZE:
                if (wParam == SIZE_MINIMIZED)
                    return 0;
                g_ResizeWidth = (UINT)LOWORD(lParam);
                g_ResizeHeight = (UINT)HIWORD(lParam);
                return 0;
            case WM_SYSCOMMAND:
                if ((wParam & 0xfff0) == SC_KEYMENU)
                    return 0;
                break;
            case WM_DESTROY:
                PostQuitMessage(0);
                return 0;
        }

        return DefWindowProcW(hWnd, msg, wParam, lParam);
    }

    // Gets the directory path of the running executable.
    static std::string get_exe_directory() {
        char path[MAX_PATH];
        GetModuleFileNameA(NULL, path, MAX_PATH);
        std::string exe_path(path);
        size_t last_slash = exe_path.find_last_of("\\/");
        if (last_slash != std::string::npos) {
            return exe_path.substr(0, last_slash + 1);
        }
        return "";
    }

    // Initializes the rendering system with ImGui and Direct3D.
    bool init() {
        ImGui_ImplWin32_EnableDpiAwareness();
        wc = { sizeof(wc), CS_CLASSDC, wnd_proc, 0L, 0L, GetModuleHandle(nullptr), nullptr, nullptr, nullptr, nullptr, L"App", nullptr };
        RegisterClassExW(&wc);
        window_hwnd = ::CreateWindowW(wc.lpszClassName, L"App window", WS_OVERLAPPEDWINDOW, 100, 100, 1280, 800, nullptr, nullptr, wc.hInstance, nullptr);

        if (!create_device_D3D(window_hwnd)) {
            cleanup_device_D3D();
            UnregisterClassW(wc.lpszClassName, wc.hInstance);
            return false;
        }

        ShowWindow(window_hwnd, SW_SHOWDEFAULT);
        UpdateWindow(window_hwnd);

        IMGUI_CHECKVERSION();
        ImGui::CreateContext();
        ImGuiIO& io = ImGui::GetIO(); (void)io;
        io.ConfigFlags |= ImGuiConfigFlags_NavEnableKeyboard;
        io.ConfigFlags |= ImGuiConfigFlags_NavEnableGamepad;

        ImGui::StyleColorsDark();

        ImGui_ImplWin32_Init(window_hwnd);
        ImGui_ImplDX11_Init(g_pd3dDevice, g_pd3dDeviceContext);

        float dpi_scale = get_window_content_scale(window_hwnd);

        std::string font_path = get_exe_directory() + "Ubuntu-Regular.ttf";
        FILE* font_file = fopen(font_path.c_str(), "rb");
        if (font_file) {
            fclose(font_file);
            ImGui::GetIO().Fonts->AddFontFromFileTTF(font_path.c_str(), 16 * dpi_scale);
        } else {
            ImGui::GetIO().Fonts->AddFontDefault();
        }

        ImGui::GetStyle().ScaleAllSizes(dpi_scale);

        initialized = true;
        return true;
    }

    // Processes Windows messages and handles window events.
    bool handle_messages() {
        MSG msg;

        while (::PeekMessage(&msg, nullptr, 0U, 0U, PM_REMOVE)) {
            TranslateMessage(&msg);
            DispatchMessage(&msg);

            if (msg.message == WM_QUIT) {
                done = true;
            }
        }

        if (done) {
            return false;
        }

        if (g_SwapChainOccluded && g_pSwapChain->Present(0, DXGI_PRESENT_TEST) == DXGI_STATUS_OCCLUDED) {
            Sleep(10);
            return false;
        }
        g_SwapChainOccluded = false;

        if (g_ResizeWidth != 0 && g_ResizeHeight != 0) {
            cleanup_render_target();
            g_pSwapChain->ResizeBuffers(0, g_ResizeWidth, g_ResizeHeight, DXGI_FORMAT_UNKNOWN, 0);
            g_ResizeWidth = g_ResizeHeight = 0;
            create_render_target();
        }

        return true;
    }

    // Prepares a new ImGui frame for rendering.
    void begin_frame() {
        ImGui_ImplDX11_NewFrame();
        ImGui_ImplWin32_NewFrame();
        ImGui::NewFrame();

        ImGui::SetNextWindowPos(ImVec2(0.0f, 0.0f));
        ImGui::SetNextWindowSize(ImGui::GetIO().DisplaySize);
    }

    // Presents the rendered frame to the screen.
    void render_frame() {
        ImGui::Render();
        static const float clear_color_with_alpha[4] = { 0, 0, 0, 0 };
        g_pd3dDeviceContext->OMSetRenderTargets(1, &g_mainRenderTargetView, nullptr);
        g_pd3dDeviceContext->ClearRenderTargetView(g_mainRenderTargetView, clear_color_with_alpha);
        ImGui_ImplDX11_RenderDrawData(ImGui::GetDrawData());

        HRESULT hr = g_pSwapChain->Present(1, 0);
        g_SwapChainOccluded = (hr == DXGI_STATUS_OCCLUDED);
    }

    // Cleans up all rendering resources.
    void cleanup() {
        if (!initialized) return;
        
        ImGui_ImplDX11_Shutdown();
        ImGui_ImplWin32_Shutdown();
        ImGui::DestroyContext();

        cleanup_device_D3D();
        DestroyWindow(window_hwnd);
        UnregisterClassW(wc.lpszClassName, wc.hInstance);
        
        initialized = false;
    }
    
    bool is_initialized() {
        return initialized;
    }

    // Adds a log message to the log display.
    void add_log(const std::string& msg, const std::initializer_list<uint8_t> color) {
        if (!initialized) {
            std::cout << "[LOG] " << msg << std::endl;
            return;
        }
        
        log_list_mtx.lock();
        log_list.emplace_back(msg, color);
        scroll_to_bottom = true;
        log_list_mtx.unlock();
    }

    // Adds a log message with a custom label to the log display.
    void add_log(const std::string& lbl, const std::string& msg, const std::initializer_list<uint8_t> color) {
        if (!initialized) {
            if (lbl.size() > 0) {
                std::cout << "[" << lbl << "] " << msg << std::endl;
            } else {
                std::cout << msg << std::endl;
            }
            return;
        }
        
        log_list_mtx.lock();
        log_list.emplace_back(lbl, msg, color, lbl.size() > 0);
        scroll_to_bottom = true;
        log_list_mtx.unlock();
    }

    // Renders the main user interface with blockchain information and controls.
    void render_ui(int local_port, const std::string& local_uuid, bool& is_mining, std::function<void()> start_mining_callback, std::function<void(const std::string&, int)> connect_callback) {
        ImGui::Begin("Block chain", nullptr, window_flags);

        ImGui::Text("Local port: %d", local_port);

        ImGui::SameLine();
        ImGui::Dummy(ImVec2(30.f, 0.f));
        ImGui::SameLine();

        ImGui::Text("UUID: %s", local_uuid.c_str());

        ImGui::SameLine();
        ImGui::Dummy(ImVec2(30.f, 0.f));
        ImGui::SameLine();

        if ((ImGui::Button("Mine") == true) && (is_mining == false)) {
            is_mining = true;
            start_mining_callback();
        }

        ImGui::Text("Remote port:");
        ImGui::SameLine();

        static char port_buf[6] = {};
        ImGui::PushItemWidth(100);
        ImGui::InputText("##remote_port", port_buf, sizeof(port_buf));
        ImGui::PopItemWidth();

        ImGui::SameLine();

        if (ImGui::Button("Connect") == true) {
            int port_num = strtol(port_buf, nullptr, 10);
            memset(port_buf, 0, sizeof(port_buf));

            connect_callback("127.0.0.1", port_num);
        }

        auto avail_region = ImGui::GetContentRegionAvail();
        ImGui::BeginChild("##logs", ImVec2(avail_region.x - 250, avail_region.y - 50.f), ImGuiChildFlags_Borders, ImGuiWindowFlags_AlwaysVerticalScrollbar);

        log_list_mtx.lock();

        ImGuiListClipper clipper;
        clipper.Begin((int)log_list.size(), ImGui::GetTextLineHeightWithSpacing());

        while (clipper.Step()) {
            for (int i = clipper.DisplayStart; i < clipper.DisplayEnd; i++) {
                const log_msg& m = log_list[i];
                ImGui::PushStyleColor(ImGuiCol_Text, m.color);

                if (m.show_label == true) {
                    ImGui::Text("[%s] %s", m.label.c_str(), m.data.c_str());
                }
                else {
                    ImGui::Text("%s", m.data.c_str());
                }

                ImGui::PopStyleColor();
            }
        }

        if (scroll_to_bottom == true) {
            scroll_to_bottom = false;
            ImGui::SetScrollHereY(1.f);
        }

        log_list_mtx.unlock();

        ImGui::EndChild();

        ImGui::SameLine();
        ImGui::BeginGroup();

        ImGui::PushStyleColor(ImGuiCol_Text, IM_COL32(127, 127, 127, 255));
        ImGui::Text("Info");
        ImGui::PopStyleColor();

        ImGui::Text(std::format("Peers: {}", blockchain::get_connection_count()).c_str());

        blockchain::block_chain_mtx.lock();
        ImGui::Text(std::format("Difficulty: {}", blockchain::current_difficulty).c_str());

        ImGui::BeginListBox("##chain", ImVec2(ImGui::GetContentRegionAvail().x, ImGui::GetContentRegionAvail().y - 50.f));

        const auto& block_list = blockchain::get_block_list();
        for (size_t i = 0; i < block_list.size(); i++) {
            const block* b = block_list[i];
            const std::string lb_entry = std::format("Block {}: {}", b->index, b->data);

            bool tmp_bool = false;
            ImGui::PushID((int)i);
            bool is_clicked = ImGui::Selectable(lb_entry.c_str(), &tmp_bool);
            ImGui::PopID();

            if (is_clicked == true) {
                std::time_t b_time = std::chrono::system_clock::to_time_t(b->timestamp);

                add_log("INFO", "Block info", { 127, 127, 255, 255 });
                add_log("", std::format("Index: {}", b->index), { 255, 255, 255, 255 });
                add_log("", std::format("Data: {}", b->data), { 255, 255, 255, 255 });
                add_log("", std::format("Hash: {}", b->hash.to_string()), { 255, 255, 255, 255 });
                add_log("", std::format("Previous hash: {}", b->previous_hash.to_string()), { 255, 255, 255, 255 });
                add_log("", std::format("Nonce: {}", b->nonce), { 255, 255, 255, 255 });
                add_log("", std::format("Difficulty: {}", b->difficulty), { 255, 255, 255, 255 });
                add_log("", std::format("Timestamp: {}", std::ctime(&b_time)), { 255, 255, 255, 255 });
            }
        }

        static size_t prev_block_list_len = 0;

        if (prev_block_list_len != block_list.size()) {
            prev_block_list_len = block_list.size();
            ImGui::SetScrollHereY(1.f);
        }

        blockchain::block_chain_mtx.unlock();

        ImGui::EndListBox();
        ImGui::EndGroup();

        ImGui::Text("Application average %.3f ms/frame (%.1f FPS)", 1000.0f / ImGui::GetIO().Framerate, ImGui::GetIO().Framerate);
        ImGui::End();
    }

};

