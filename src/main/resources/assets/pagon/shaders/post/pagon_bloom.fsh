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

vec4 getFill(vec3 centerColor, float alpha) {
    if (u_FillMode == 1) {
        float rainbowRepeats = max(u_RainbowScale, 0.1);
        float time = (u_ShaderTime / 4000.0) * u_RainbowSpeed;
        float verticalProgress = fract((gl_FragCoord.y / u_Resolution.y) * rainbowRepeats - time);
        float hue = verticalProgress;
        vec3 finalColor = hsv2rgb(vec3(hue, 0.8, 1.0));
        finalColor = applyRainbowAdjustments(finalColor);
        return vec4(finalColor, alpha);
    }

    return vec4(centerColor, alpha);
}

vec3 getSobelColor(vec2 uv) {
    vec2 oneTexel = 1.0 / InSize;
    for (int r = 1; r <= u_GlowQuality * int(u_Width); r += u_GlowQuality) {
        float rf = float(r);
        vec2 dx = vec2(oneTexel.x * rf, 0.0);
        vec2 dy = vec2(0.0, oneTexel.y * rf);
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

float blur() {
    if (u_Width == 0.0) {
        return 0.0;
    }
    vec2 oneTexel = 1.0 / InSize;
    int quality = max(1, u_GlowQuality);
    int w = quality * int(u_Width);
    float blurred = 0.0;
    blurred += sign(texture(InSampler, texCoord + oneTexel * vec2( w, 0)).a);
    blurred += sign(texture(InSampler, texCoord + oneTexel * vec2(-w, 0)).a);
    blurred += sign(texture(InSampler, texCoord + oneTexel * vec2( 0, w)).a);
    blurred += sign(texture(InSampler, texCoord + oneTexel * vec2( 0,-w)).a);
    blurred += sign(texture(InSampler, texCoord + oneTexel * vec2( w, w)).a);
    blurred += sign(texture(InSampler, texCoord + oneTexel * vec2( w,-w)).a);
    blurred += sign(texture(InSampler, texCoord + oneTexel * vec2(-w, w)).a);
    blurred += sign(texture(InSampler, texCoord + oneTexel * vec2(-w,-w)).a);
    if (int(u_Width) > 2 && blurred == 0.0) {
        return 0.0;
    }
    for (int x = -w; x <= w; x += quality) {
        for (int y = -w; y <= w; y += quality) {
            if (x == 0 && y == 0) continue;
            if ((abs(x) == w && abs(y) == w) || (abs(x) == w && y == 0) || (abs(y) == w && x == 0)) continue;
            blurred += sign(texture(InSampler, texCoord + oneTexel * vec2(float(x), float(y))).a);
        }
    }
    return clamp(blurred / max(float(((int(u_Width) * int(u_Width)) + int(u_Width)) * 4), 1.0), 0.0, 1.0) * u_GlowMultiplier;
}

void main() {
    vec2 oneTexel = 1.0 / InSize;
    vec4 center = texture(InSampler, texCoord);

    if (center.a > 0.0) {
        vec4 fill = getFill(center.rgb, u_FillAlpha);
        if (u_GlowInside == 1) {
            vec4 outline = vec4(center.rgb, u_OutlineAlpha);
            fragColor = mix(fill, outline, u_GlowMultiplier - blur());
        } else {
            fragColor = fill;
        }
        return;
    }

    float glow = blur();
    if (glow == 0.0) {
        discard;
    }

    bool edge = false;
    for (int x = -1; x <= 1 && !edge; ++x) {
        for (int y = -1; y <= 1 && !edge; ++y) {
            if (x == 0 && y == 0) continue;
            if (texture(InSampler, texCoord + vec2(float(x), float(y)) * oneTexel).a > 0.0) {
                edge = true;
            }
        }
    }

    vec3 outlineRGB = u_FillMode == 1 ? vec3(getFill(center.rgb, u_OutlineAlpha)) : getSobelColor(texCoord);
    if (outlineRGB.r + outlineRGB.g + outlineRGB.b == 0.0) {
        int w = max(1, u_GlowQuality * int(u_Width));
        vec2 dx = vec2(oneTexel.x * float(w), 0.0);
        vec2 dy = vec2(0.0, oneTexel.y * float(w));
        vec4 s = texture(InSampler, texCoord + dx);
        if (s.a > 0.0) outlineRGB = s.rgb;
        s = texture(InSampler, texCoord - dx);
        if (s.a > 0.0) outlineRGB = s.rgb;
        s = texture(InSampler, texCoord + dy);
        if (s.a > 0.0) outlineRGB = s.rgb;
        s = texture(InSampler, texCoord - dy);
        if (s.a > 0.0) outlineRGB = s.rgb;
    }

    fragColor = edge ? vec4(outlineRGB, u_OutlineAlpha) : vec4(outlineRGB, glow * u_OutlineAlpha);
}

