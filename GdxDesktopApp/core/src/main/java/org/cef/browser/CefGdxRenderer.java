package org.cef.browser;


import com.badlogic.gdx.Gdx;
import org.cef.handler.CefAcceleratedPaintInfo;
import org.lwjgl.opengl.EXTMemoryObject;
import org.lwjgl.opengl.EXTMemoryObjectWin32;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.WGL;

import static com.badlogic.gdx.graphics.GL20.GL_NO_ERROR;
import static com.jogamp.opengl.GL.GL_BGRA8;



public class CefGdxRenderer {


    //Shared render context info
    private long gl_hdc = -1;
    private long gl_hglrc = -1;

    private boolean gl_thread_init = false;

    private int g_shared_texture = -1;


    public CefGdxRenderer(){}


    //Must be set!
    public void setGLContext(long hdc, long hglrc){
        gl_hdc = hdc;
        gl_hglrc = hglrc;
    }


    public int getSharedTexture(){
        return g_shared_texture;
    }


    public void onAcceleratedPaint(CefAcceleratedPaintInfo info, int width, int height){

        //This is called on CEF render thread!
        //Lock shared context mutex, copy DirectX texture, release mutex
        try {
            Gdx.g_window_mutex.acquire();
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
            copy_shared_texture(info, width, height);
        }
        catch (Exception e) {
            System.out.printf("CefGdxRenderer::copy_shared_texture() error: %s\n", e.toString());
        }
        finally {

            //Clear context
            if(!WGL.wglMakeCurrent(0, 0)){
                System.out.printf("CefGdxRenderer::onAcceleratedPaint() failed to clear current context: %d\n", Gdx.gl.glGetError());
            }

            Gdx.g_window_mutex.release();
        }

    }


    private void copy_shared_texture(CefAcceleratedPaintInfo info, int width, int height){

        if(gl_hglrc == -1){
            System.out.println("CefGdxRenderer::copy_shared_texture(): no context!");
            return;
        }


        //Set context
        if(!WGL.wglMakeCurrent(gl_hdc, gl_hglrc)){

            //Retry, as sometimes first call fails from previous error for some reason?!
            if(!WGL.wglMakeCurrent(gl_hdc, gl_hglrc)){
                System.out.printf("CefGdxRenderer::copy_shared_texture(): Failed to make current context: %d\n", GL32.glGetError());
                return;
            }
        }


        //Init GL thread context
        if(!gl_thread_init){
            gl_thread_init = true;
            org.lwjgl.opengl.GL.createCapabilities();
        }



        GL32.glEnable(GL32.GL_BLEND);
        checkGLError("glEnable");


        //Create texture (has to be recreated each frame as it cannot be reused)
        int sharedTextureId = GL32.glGenTextures();
        checkGLError("glGenTextures");


        int memoryObject = EXTMemoryObject.glCreateMemoryObjectsEXT();

        if (memoryObject == 0) {
            System.out.println("CefGdxRenderer::copy_shared_texture(): failed to create memory object for shared texture!");
            GL32.glDeleteTextures(sharedTextureId);
            return;
        }


        //The size of the texture we get from CEF. The CEF format is CEF_COLOR_TYPE_BGRA_8888
        //It has 4 bytes per pixel. The mem object requires this to be multiplied with 2
        long size = (long)width * height * 4 * 2;

        //Cef uses the GL_HANDLE_TYPE_D3D11_IMAGE_EXT handle for their shared texture
        //Import the shared texture to the memory object
        EXTMemoryObjectWin32.glImportMemoryWin32HandleEXT(memoryObject,
            size,
            EXTMemoryObjectWin32.GL_HANDLE_TYPE_D3D11_IMAGE_EXT,
            info.shared_texture_handle
        );

        int error = GL32.glGetError();

        if (error != GL_NO_ERROR) {
            System.out.printf("CefGdxRenderer::copy_shared_texture(): glImportMemoryWin32HandleEXT failed with error: %d", error);

            GL32.glDeleteTextures(sharedTextureId);
            EXTMemoryObject.glDeleteMemoryObjectsEXT(memoryObject); // If memory object was created
            return;
        }


        GL32.glBindTexture(GL32.GL_TEXTURE_2D, sharedTextureId);
        checkGLError("glBindTexture");

        EXTMemoryObject.glTexStorageMem2DEXT(
            GL32.GL_TEXTURE_2D,     // Target (not texture ID)
            1,                      // Mip levels
            GL_BGRA8,               // Internal format
            width,
            height,
            memoryObject,
            0                       // Offset
        );

        error = GL32.glGetError();

        if (error != GL_NO_ERROR) {
            System.out.printf("CefGdxRenderer::copy_shared_texture(): glTexStorageMem2DEXT failed with error: %d", error);
            // Cleanup the resources created so far
            GL32.glDeleteTextures(sharedTextureId);
            EXTMemoryObject.glDeleteMemoryObjectsEXT(memoryObject); // If memory object was created
            return;
        }

        GL32.glFinish();
        checkGLError("glFinish");

        /*
        GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MIN_FILTER, GL32.GL_LINEAR);
        GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MAG_FILTER, GL32.GL_LINEAR);
        GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_WRAP_S, GL32.GL_CLAMP_TO_EDGE);
        GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_WRAP_T, GL32.GL_CLAMP_TO_EDGE);
        checkGLError("glTexParameteri");
        */


        //Free memory object
        EXTMemoryObject.glDeleteMemoryObjectsEXT(memoryObject);
        checkGLError("glDeleteMemoryObjectsEXT");


        //Update shared texture id
        int old_g_shared_texture = g_shared_texture;
        g_shared_texture = sharedTextureId;

        if(old_g_shared_texture != -1){
            GL32.glDeleteTextures(old_g_shared_texture); //Delete after swap!
            checkGLError("glDeleteTextures");
        }

        GL32.glBindTexture(GL32.GL_TEXTURE_2D, 0);
        checkGLError("glBindTexture");

    }


    private void checkGLError(String msg){
        int err = GL32.glGetError();

        if(err != GL_NO_ERROR){
            //System.out.printf("GL error %d after %s\n", err, msg);
        }
    }
}
