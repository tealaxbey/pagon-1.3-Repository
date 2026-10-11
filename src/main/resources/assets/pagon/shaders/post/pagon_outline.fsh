#version 330

#define TAU 6.28318530718

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform ShaderConfig {
    float u_ShaderTime;
    vec2 u_Resolution;
    float u_Width;
    int u_GlowInside;
    int u_GlowQuality;
    int u_FillMode;
    float u_GlowMultiplier;
    float u_FillAlpha;
    float u_OutlineAlpha;
    float u_RainbowScale;
    float u_RainbowSpeed;
    float u_RainbowBrightness;
    float u_RainbowContrast;
};

in vec2 texCoord;

out vec4 fragColor;

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

vec3 applyRainbowAdjustments(vec3 color) {
    color = ((color - 0.5) * u_RainbowContrast) + 0.5;
    color *= u_RainbowBrightness;
    return clamp(color, 0.0, 1.0);
}

vec4 getFill(vec3 centerColor) {
    if (u_FillMode == 1) {
        float rainbowRepeats = max(u_RainbowScale, 0.1);
        float time = (u_ShaderTime / 4000.0) * u_RainbowSpeed;
        float verticalProgress = fract((gl_FragCoord.y / u_Resolution.y) * rainbowRepeats - time);
        float hue = verticalProgress;
        vec3 finalColor = hsv2rgb(vec3(hue, 0.8, 1.0));
        finalColor = applyRainbowAdjustments(finalColor);
        return vec4(finalColor, u_FillAlpha);
    }

    return vec4(centerColor, u_FillAlpha);
}

vec3 getSobelColor(vec2 uv) {
    vec2 oneTexel = 1.0 / InSize;
    for (int r = 1; r <= int(ceil(u_Width)); ++r) {
        vec2 dx = vec2(oneTexel.x * float(r), 0.0);
        vec2 dy = vec2(0.0, oneTexel.y * float(r));
        vec4 s = texture(InSampler, uv - dx);
        if (s.a > 0.0) return s.rgb;
        s = texture(InSampler, uv + dx);
        if (s.a > 0.0) return s.rgb;
        s = texture(InSampler, uv - dy);
        if (s.a > 0.0) return s.rgb;
        s = texture(InSampler, uv + dy);
        if (s.a > 0.0) return s.rgb;
        vec2 od = vec2(dx.x, dy.y);
        s = texture(InSampler, uv + od);
        if (s.a > 0.0) return s.rgb;
        s = texture(InSampler, uv + vec2(od.x, -od.y));
        if (s.a > 0.0) return s.rgb;
        s = texture(InSampler, uv + vec2(-od.x, od.y));
        if (s.a > 0.0) return s.rgb;
        s = texture(InSampler, uv - od);
        if (s.a > 0.0) return s.rgb;
    }
    return vec3(0.0);
}

void main() {
    vec2 oneTexel = 1.0 / InSize;
    vec2 dx = vec2(oneTexel.x * u_Width, 0.0);
    vec2 dy = vec2(0.0, oneTexel.y * u_Width);
    vec4 center = texture(InSampler, texCoord);
    vec4 left = texture(InSampler, texCoord - dx);
    vec4 right = texture(InSampler, texCoord + dx);
    vec4 up = texture(InSampler, texCoord - dy);
    vec4 down = texture(InSampler, texCoord + dy);
    float edge = clamp(abs(center.a - left.a) + abs(center.a - right.a) + abs(center.a - up.a) + abs(center.a - down.a), 0.0, 1.0);

    if (center.a > 0.0) {
        fragColor = getFill(center.rgb);
        return;
    }

    if (edge > 0.0) {
        vec3 outlineRGB = u_FillMode == 1 ? vec3(getFill(center.rgb)) : getSobelColor(texCoord);
        fragColor = vec4(outlineRGB, edge * u_OutlineAlpha);
    } else {
        fragColor = vec4(0.0);
    }
}

