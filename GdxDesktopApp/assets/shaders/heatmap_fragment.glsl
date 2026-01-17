#version 120

uniform sampler2D u_source;
uniform vec3[5] u_colorMap;

varying vec2 v_texcoord;



float fade(float low, float high, float value){
    float mid = (low+high)*0.5;
    float range = (high-low)*0.5;
    float x = 1.0 - clamp(abs(mid-value)/range, 0.0, 1.0);
    return smoothstep(0.0, 1.0, x);
}

vec3 getColor(float intensity){
    vec3 color = (
        fade(-0.25, 0.25, intensity) * u_colorMap[0] +
        fade(0.0, 0.5, intensity) * u_colorMap[1] +
        fade(0.25, 0.75, intensity) * u_colorMap[2] +
        fade(0.5, 1.0, intensity) * u_colorMap[3] +
        smoothstep(0.75, 1.0, intensity) * u_colorMap[4]
    );

    return color;
}

vec4 alphaFun(vec3 color, float intensity){
    float alpha = smoothstep(0.0, 1.0, intensity);
    return vec4(color * alpha, alpha);
}

void main(){
    float intensity = smoothstep(0.0, 1.0, texture2D(u_source, v_texcoord).r);
    vec3 color = getColor(intensity);
    gl_FragColor = alphaFun(color, intensity);
}
