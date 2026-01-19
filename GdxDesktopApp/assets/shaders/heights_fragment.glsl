#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_off;
varying vec2 v_dim;
varying float v_intensity;


void main() {
    float falloff = 1.0 - smoothstep(0.0, 1.0, length(v_off/v_dim));
    float intensity = falloff * v_intensity;
    gl_FragColor = vec4(intensity);
}
