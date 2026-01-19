attribute vec4 a_position;

uniform mat4 u_projTrans;
uniform vec4 u_inColor;
varying vec4 v_color;

void main()
{
    v_color = u_inColor; //Forward input color
    v_color.a = v_color.a * (256.0 / 255.0);
    gl_Position = u_projTrans * a_position;
}
