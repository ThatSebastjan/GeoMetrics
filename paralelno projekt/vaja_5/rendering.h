#ifndef RENDERING_INCLUDE
#define RENDERING_INCLUDE

#include "imgui.h"
#include "imgui_impl_win32.h"
#include "imgui_impl_dx11.h"
#include <d3d11.h>
#include <tchar.h>
#include <string>
#include <vector>
#include <mutex>
#include <cstdint>
#include <functional>
#pragma comment(lib, "d3d11.lib")

extern IMGUI_IMPL_API LRESULT ImGui_ImplWin32_WndProcHandler(HWND hWnd, UINT msg, WPARAM wParam, LPARAM lParam);

namespace rendering {

    extern bool done;
    extern ImGuiWindowFlags window_flags;

    struct log_msg {
        std::string label;
        std::string data;
        uint32_t color;
        bool show_label;

        log_msg(const std::string& m, const std::initializer_list<uint8_t> c);
        log_msg(const std::string& lbl, const std::string& m, const std::initializer_list<uint8_t> c, bool s);
    };

    bool init();
    bool is_initialized();
    bool handle_messages();
    void begin_frame();
    void render_frame();
    void cleanup();
    void add_log(const std::string& msg, const std::initializer_list<uint8_t> color = { 255, 255, 255, 127 });
    void add_log(const std::string& lbl, const std::string& msg, const std::initializer_list<uint8_t> color = { 255, 255, 255, 127 });
    void render_ui(int local_port, const std::string& local_uuid, bool& is_mining, unsigned int* num_mining_threads, std::function<void()> start_mining_callback, std::function<void(const std::string&, int)> connect_callback);

};

#endif
