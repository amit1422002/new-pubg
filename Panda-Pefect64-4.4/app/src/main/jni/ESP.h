#ifndef BETA_ESP_IMPORTANT_ESP_H
#define BETA_ESP_IMPORTANT_ESP_H
#include "struct.h"

class ESP {
private:
    JNIEnv *_env;
    jobject _cvsView;
    jobject _cvs;
    jclass canvasView;
    jmethodID drawline;
    jmethodID drawrect;
    jmethodID drawfilledrect;
    jmethodID drawfilledrect2;
    jmethodID drawcurverect;


public:
    ESP() {
        _env = nullptr;
        _cvsView = nullptr;
        _cvs = nullptr;
        canvasView = nullptr;
        drawline = nullptr;
        drawrect = nullptr;
        drawfilledrect= nullptr;
        drawfilledrect2= nullptr;
        drawcurverect = nullptr;
    }

    ESP(JNIEnv *env, jobject cvsView, jobject cvs) {
        this->_env = env;
        this->_cvsView = cvsView;
        this->_cvs = cvs;
        canvasView = _env->GetObjectClass(_cvsView);
        drawline = _env->GetMethodID(canvasView, "DrawLine",
                                     "(Landroid/graphics/Canvas;IIIIFFFFF)V");
        drawrect = _env->GetMethodID(canvasView, "DrawRect",
                                     "(Landroid/graphics/Canvas;IIIIFFFFF)V");
        drawcurverect = _env->GetMethodID(canvasView, "DrawCurveRect",
                                          "(Landroid/graphics/Canvas;IIIIFFFFF)V");
        drawfilledrect= _env->GetMethodID(canvasView, "DrawFilledRect",
                                          "(Landroid/graphics/Canvas;IIIIFFFF)V");
        drawfilledrect2= _env->GetMethodID(canvasView, "DrawFilledRect2",
                                           "(Landroid/graphics/Canvas;IIIIFFFF)V");
    }

    bool isValid() const {
        return (_env != nullptr && _cvsView != nullptr && _cvs != nullptr);
    }

    int getWidth() const {
        if (isValid()) {
            jclass canvas = _env->GetObjectClass(_cvs);
            jmethodID width = _env->GetMethodID(canvas, "getWidth", "()I");
            _env->DeleteLocalRef(canvas);
            return _env->CallIntMethod(_cvs, width);

        }
        return 0;
    }

    int getHeight() const {
        if (isValid()) {
            jclass canvas = _env->GetObjectClass(_cvs);
            jmethodID width = _env->GetMethodID(canvas, "getHeight", "()I");
            _env->DeleteLocalRef(canvas);
            return _env->CallIntMethod(_cvs, width);
        }
        return 0;
    }

    void DrawLine(Color color, float thickness, Vec2 start, Vec2 end) {
        if (isValid()) {
            _env->CallVoidMethod(_cvsView, drawline, _cvs, (int) color.a, (int) color.r,
                                 (int) color.g, (int) color.b,
                                 thickness,
                                 start.x, start.y, end.x, end.y);
        }
    }

    void DrawRect(Color color, float thickness, Vec2 start, Vec2 end) {
        if (isValid()) {
            _env->CallVoidMethod(_cvsView, drawrect, _cvs, (int) color.a, (int) color.r,
                                 (int) color.g, (int) color.b,
                                 thickness,
                                 start.x, start.y, end.x, end.y);
        }
    }

    void DrawCurveRect(Color color, float thickness, Vec2 start, Vec2 end) {
        if (isValid()) {
            _env->CallVoidMethod(_cvsView, drawcurverect, _cvs, (int) color.a, (int) color.r,
                                 (int) color.g, (int) color.b,
                                 thickness,
                                 start.x, start.y, end.x, end.y);
        }
    }


    void DrawFilledRect2(Color color, Vec2 start, Vec2 end) {
        if (isValid()) {
            _env->CallVoidMethod(_cvsView, drawfilledrect, _cvs, (int) color.a, (int) color.r,
                                 (int) color.g, (int) color.b,
                                 start.x, start.y, end.x, end.y);
        }
    }


    void DrawFilledRect(Color color, Vec2 start, Vec2 end) {
        if (isValid()) {
            _env->CallVoidMethod(_cvsView, drawfilledrect, _cvs, (int) color.a, (int) color.r,
                                 (int) color.g, (int) color.b,
                                 start.x, start.y, end.x, end.y);
        }
    }

    void DrawFilledRoundRect(Color color, Vec2 start, Vec2 end) {
        if (isValid()) {
            jclass canvasView = _env->GetObjectClass(_cvsView);
            jmethodID drawfilledroundrect= _env->GetMethodID(canvasView, "DrawFilledRoundRect",
                                                             "(Landroid/graphics/Canvas;IIIIFFFF)V");
            _env->CallVoidMethod(_cvsView, drawfilledroundrect, _cvs, (int) color.a, (int) color.r,
                                 (int) color.g, (int) color.b,
                                 start.x, start.y, end.x, end.y);
        }
    }

    void DrawTransRoundRect(Color color, Vec2 start, Vec2 end) {
        if (isValid()) {
            jclass canvasView = _env->GetObjectClass(_cvsView);
            jmethodID drawfilledroundrect= _env->GetMethodID(canvasView, "DrawTransRoundRect",
                                                             "(Landroid/graphics/Canvas;IIIIFFFF)V");
            _env->CallVoidMethod(_cvsView, drawfilledroundrect, _cvs, (int) color.a, (int) color.r,
                                 (int) color.g, (int) color.b,
                                 start.x, start.y, end.x, end.y);
        }
    }

    void DrawFillCircle(Color color, Vec2 pos, float radius,float thickness) {
        if (isValid()) {
            jmethodID drawfilledcircle = _env->GetMethodID(canvasView, "DrawFillCircle",
                                                           "(Landroid/graphics/Canvas;IIIIFFFF)V");

            _env->CallVoidMethod(_cvsView, drawfilledcircle, _cvs, (int) color.a, (int) color.r,
                                 (int) color.g, (int) color.b, pos.x, pos.y, radius,thickness);
        }
    }

    void DrawTransCircle(Color color, Vec2 start, Vec2 end , float radius,float thickness) {
        if (isValid()) {
            jmethodID drawfilledcircle = _env->GetMethodID(canvasView, "DrawFillCircle",
                                                           "(Landroid/graphics/Canvas;IIIIFFFF)V");

            _env->CallVoidMethod(_cvsView, drawfilledcircle, _cvs, (int) color.a, (int) color.r,
                                 (int) color.g, (int) color.b, start.x, start.y, end.x, end.y, radius,thickness);
        }
    }

    void DrawTranslucentRoundRect(Color color, Vec2 start, Vec2 end , float radius,float thickness) {
        if (isValid()) {
            jclass canvasView = _env->GetObjectClass(_cvsView);
            jmethodID drawfilledcircle = _env->GetMethodID(canvasView, "DrawFillCircle",
                                                           "(Landroid/graphics/Canvas;IIIIFFFF)V");
            _env->CallVoidMethod(_cvsView, drawfilledcircle, _cvs, (int) color.a, (int) color.r,
                                 (int) color.g, (int) color.b,
                                 start.x, start.y, end.x, end.y,  radius,thickness);
        }
    }

    void DrawCircle(Color color, Vec2 pos, float radius,float thickness) {
        if (isValid()) {
            jmethodID drawcircle = _env->GetMethodID(canvasView, "DrawCircle",
                                                     "(Landroid/graphics/Canvas;IIIIFFFF)V");

            _env->CallVoidMethod(_cvsView, drawcircle, _cvs, (int) color.a, (int) color.r,
                                 (int) color.g, (int) color.b, pos.x, pos.y, radius,thickness);
        }
    }



    void DrawFilledTriangle(Color color, Vec2 pos, float size) {
        if (isValid()) {
            jclass canvasView = _env->GetObjectClass(_cvsView);
            jmethodID drawFilledTriangle = _env->GetMethodID(canvasView, "DrawFilledTriangle",
                                                             "(Landroid/graphics/Canvas;IIIIFFF)V");
            _env->CallVoidMethod(_cvsView, drawFilledTriangle, _cvs, color.a, color.r,
                                 color.g, color.b, pos.x, pos.y, size);
        }
    }

    void DrawFilledCircle(Color color, Vec2 pos, float radius) {
        if (isValid()) {
            jclass canvasView = _env->GetObjectClass(_cvsView);
            jmethodID drawfilledcircle = _env->GetMethodID(canvasView, "DrawFilledCircle",
                                                           "(Landroid/graphics/Canvas;IIIIFFF)V");
            _env->CallVoidMethod(_cvsView, drawfilledcircle, _cvs, (int) color.a, (int) color.r,
                                 (int) color.g, (int) color.b, pos.x, pos.y, radius);
        }
    }
    
   void DrawEnemyCount(Color color, Vec2 start, Vec2 end) {
        if (isValid()) {
            jclass canvasView = _env->GetObjectClass(_cvsView);
            jmethodID drawline = _env->GetMethodID(canvasView, "DrawEnemyCount",
                                                   "(Landroid/graphics/Canvas;IIIIIIII)V");
            _env->CallVoidMethod(_cvsView, drawline, _cvs, (int) color.a, (int) color.r,
                                 (int) color.g, (int) color.b, (int) start.x, (int) start.y, 
                                 (int) end.x, (int) end.y);
        }
    }

    float measureTextWidth(const char* text, float textSize) {
        if (!isValid()) return 0.0f;
        jclass canvasView = _env->GetObjectClass(_cvsView);
        jmethodID measureMethod = _env->GetMethodID(canvasView, "measureTextWidth", "(Ljava/lang/String;F)F");
        if (!measureMethod) {
            _env->DeleteLocalRef(canvasView);
            return 0.0f;
        }
        jstring jText = _env->NewStringUTF(text);
        float width = _env->CallFloatMethod(_cvsView, measureMethod, jText, textSize);
        _env->DeleteLocalRef(jText);
        _env->DeleteLocalRef(canvasView);
        return width;
    }

    void DebugText(char * s){
        jmethodID mid = _env->GetMethodID(canvasView, "DebugText", "(Ljava/lang/String;)V");
        jstring name = _env->NewStringUTF(s);
        _env->CallVoidMethod(_cvsView, mid, name);
        _env->DeleteLocalRef(name);
    }


    /*void DrawName(Color color, const char *txt,int teamid, Vec2 pos, float size) {
        if (isValid()) {
            jmethodID drawtext = _env->GetMethodID(canvasView, "DrawName",
                                                   "(Landroid/graphics/Canvas;IIIILjava/lang/String;IFFF)V");
            jstring s=_env->NewStringUTF(txt);
            _env->CallVoidMethod(_cvsView, drawtext, _cvs, (int) color.a, (int) color.r,
                                 (int) color.g, (int) color.b,
                                 s,teamid, pos.x, pos.y, size);
            _env->DeleteLocalRef(s);
        }

    }*/

    void DrawName(Color color, const char *txt, int teamid, Vec2 pos, float size) {
        if (isValid() && txt != nullptr) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawtext = _env->GetMethodID(cvsClass, "DrawName",
                                                   "(Landroid/graphics/Canvas;IIIILjava/lang/String;IFFF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawtext != nullptr) {
                jstring s = _env->NewStringUTF(txt);
                _env->CallVoidMethod(_cvsView, drawtext, _cvs, (int)color.a, (int)color.r,
                                     (int)color.g, (int)color.b,
                                     s, teamid, pos.x, pos.y, size);
                _env->DeleteLocalRef(s);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawText(Color color, const char *txt, Vec2 pos, float size) {
        if (isValid() && txt != nullptr) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawtext = _env->GetMethodID(cvsClass, "DrawText",
                                                   "(Landroid/graphics/Canvas;IIIILjava/lang/String;FFF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawtext != nullptr) {
                jstring s = _env->NewStringUTF(txt);
                _env->CallVoidMethod(_cvsView, drawtext, _cvs, (int) color.a, (int) color.r,
                                     (int) color.g, (int) color.b,
                                     s, pos.x, pos.y, size);
                _env->DeleteLocalRef(s);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawTexti(Color color, const char *txt, Vec2 pos, float size) {
        DrawText(color, txt, pos, size);
    }

    void DrawTriangle(Color color, Vec2 center, float size, float angle) {
        if (isValid()) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawTriangle = _env->GetMethodID(cvsClass, "DrawTriangle",
                                                       "(Landroid/graphics/Canvas;IIIIFFFF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawTriangle != nullptr) {
                float halfSize = size / 2;
                float tipSize = size * 0.8f;
                float tipX = center.x + (cos(angle * M_PI / 180.0f) * tipSize);
                float tipY = center.y + (sin(angle * M_PI / 180.0f) * tipSize);
                _env->CallVoidMethod(_cvsView, drawTriangle, _cvs, (int)color.a, (int)color.r,
                                     (int)color.g, (int)color.b, tipX, tipY, size, angle);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawTriangleFilled(Color color, Vec2 point1, Vec2 point2, Vec2 point3) {
        if (isValid()) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawTriangleFilled = _env->GetMethodID(cvsClass, "DrawTriangleFilled",
                                                             "(Landroid/graphics/Canvas;IIIIFFFFFF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawTriangleFilled != nullptr) {
                _env->CallVoidMethod(_cvsView, drawTriangleFilled, _cvs, (int) color.a, (int) color.r,
                                     (int) color.g, (int) color.b,
                                     point1.x, point1.y,
                                     point2.x, point2.y,
                                     point3.x, point3.y);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawTexture(Color color, const char *txt, Vec2 pos, float size) {
        if (isValid() && txt != nullptr) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawtext = _env->GetMethodID(cvsClass, "DrawTexture",
                                                   "(Landroid/graphics/Canvas;IIIILjava/lang/String;FFF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawtext != nullptr) {
                jstring s = _env->NewStringUTF(txt);
                _env->CallVoidMethod(_cvsView, drawtext, _cvs, (int) color.a, (int) color.r,
                                     (int) color.g, (int) color.b,
                                     s, pos.x, pos.y, size);
                _env->DeleteLocalRef(s);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawTextName(Color color, const char *txt, Vec2 pos, float size) {
        if (isValid() && txt != nullptr) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawtext = _env->GetMethodID(cvsClass, "DrawTextName",
                                                   "(Landroid/graphics/Canvas;IIIILjava/lang/String;FFF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawtext != nullptr) {
                jstring s = _env->NewStringUTF(txt);
                _env->CallVoidMethod(_cvsView, drawtext, _cvs, (int) color.a, (int) color.r,
                                     (int) color.g, (int) color.b,
                                     s, pos.x, pos.y, size);
                _env->DeleteLocalRef(s);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawTextMode(Color color, const char *txt, Vec2 pos, float size) {
        if (isValid() && txt != nullptr) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawtext = _env->GetMethodID(cvsClass, "DrawTextMode",
                                                   "(Landroid/graphics/Canvas;IIIILjava/lang/String;FFF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawtext != nullptr) {
                jstring s = _env->NewStringUTF(txt);
                _env->CallVoidMethod(_cvsView, drawtext, _cvs, (int) color.a, (int) color.r,
                                     (int) color.g, (int) color.b,
                                     s, pos.x, pos.y, size);
                _env->DeleteLocalRef(s);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawTextMode2(Color color, const char *txt, Vec2 pos, float size) {
        DrawTextMode(color, txt, pos, size);
    }

    void DrawFillRect(Color color, Vec2 start, Vec2 end) {
        if (isValid()) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawfilledrectM = _env->GetMethodID(cvsClass, "DrawFillRect",
                                                          "(Landroid/graphics/Canvas;IIIIIIII)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawfilledrectM != nullptr) {
                _env->CallVoidMethod(_cvsView, drawfilledrectM, _cvs, (int) color.a, (int) color.r,
                                     (int) color.g, (int) color.b, (int) start.x, (int) start.y,
                                     (int) end.x, (int) end.y);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawFilledRect1(Color color, Vec2 start, Vec2 end) {
        if (isValid()) {
            _env->CallVoidMethod(_cvsView, drawfilledrect, _cvs, (int) color.a, (int) color.r,
                                 (int) color.g, (int) color.b,
                                 start.x, start.y, end.x, end.y);
        }
    }

    void DrawFilledName(Color color, Vec2 start, Vec2 end) {
        if (isValid()) {
            _env->CallVoidMethod(_cvsView, drawfilledrect, _cvs, (int) color.a, (int) color.r,
                                 (int) color.g, (int) color.b,
                                 start.x, start.y, end.x, end.y);
        }
    }

    void DrawFilledCurve(Color color, Vec2 start, Vec2 end) {
        if (isValid()) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawfilledrectM = _env->GetMethodID(cvsClass, "DrawFilledCurve",
                                                          "(Landroid/graphics/Canvas;IIIIIIII)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawfilledrectM != nullptr) {
                _env->CallVoidMethod(_cvsView, drawfilledrectM, _cvs, (int) color.a, (int) color.r,
                                     (int) color.g, (int) color.b, (int) start.x, (int) start.y, (int) end.x, (int) end.y);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawTeamID(Color color, int teamid, Vec2 pos, float size) {
        if (isValid()) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawtext = _env->GetMethodID(cvsClass, "DrawTeamID",
                                                   "(Landroid/graphics/Canvas;IIIIIFFF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawtext != nullptr) {
                _env->CallVoidMethod(_cvsView, drawtext, _cvs, (int) color.a, (int) color.r,
                                     (int) color.g, (int) color.b,
                                     teamid, pos.x, pos.y, size);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawTriangle(Color color, float x, float y, float x2, float y2, float x3, float y3) {
        if (isValid()) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawwarning = _env->GetMethodID(cvsClass, "DrawTriangle",
                                                      "(Landroid/graphics/Canvas;IIIIFFFFFFF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawwarning != nullptr) {
                _env->CallVoidMethod(_cvsView, drawwarning, _cvs, (int) color.a, (int) color.r,
                                     (int) color.g, (int) color.b, x, y, x2, y2, x3, y3, 1.0f);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawPlayerName(Color color, const char *txt, Vec2 pos, float size) {
        if (isValid() && txt != nullptr) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawtext = _env->GetMethodID(cvsClass, "DrawPlayerName",
                                                   "(Landroid/graphics/Canvas;IIIILjava/lang/String;FFF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawtext != nullptr) {
                jstring s = _env->NewStringUTF(txt);
                _env->CallVoidMethod(_cvsView, drawtext, _cvs, (int) color.a, (int) color.r,
                                     (int) color.g, (int) color.b,
                                     s, pos.x, pos.y, size);
                _env->DeleteLocalRef(s);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawCustom(Color color, const char *txt, Vec2 pos, float size) {
        if (isValid() && txt != nullptr) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawtext = _env->GetMethodID(cvsClass, "DrawCustom",
                                                   "(Landroid/graphics/Canvas;IIIILjava/lang/String;FFF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawtext != nullptr) {
                jstring s = _env->NewStringUTF(txt);
                _env->CallVoidMethod(_cvsView, drawtext, _cvs, (int) color.a, (int) color.r,
                                     (int) color.g, (int) color.b,
                                     s, pos.x, pos.y, size);
                _env->DeleteLocalRef(s);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawItems(const char *txt, float distance, Vec2 pos, float size) {
        if (isValid() && txt != nullptr) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawtext = _env->GetMethodID(cvsClass, "DrawItems",
                                                   "(Landroid/graphics/Canvas;Ljava/lang/String;FFFF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawtext != nullptr) {
                jstring s = _env->NewStringUTF(txt);
                _env->CallVoidMethod(_cvsView, drawtext, _cvs,
                                     s, distance, pos.x, pos.y, size);
                _env->DeleteLocalRef(s);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawVehicles(const char *txt, float distance, float health, float fuel, Vec2 pos, float size) {
        if (isValid() && txt != nullptr) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawtext = _env->GetMethodID(cvsClass, "DrawVehicles",
                                                   "(Landroid/graphics/Canvas;Ljava/lang/String;FFFFFF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawtext != nullptr) {
                jstring s = _env->NewStringUTF(txt);
                _env->CallVoidMethod(_cvsView, drawtext, _cvs,
                                     s, distance, health, fuel, pos.x, pos.y, size);
                _env->DeleteLocalRef(s);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawWeapon(Color color, int wid, int ammo, int ammo2, Vec2 pos, float size) {
        if (isValid()) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawtext = _env->GetMethodID(cvsClass, "DrawWeapon",
                                                   "(Landroid/graphics/Canvas;IIIIIIIFFF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawtext != nullptr) {
                _env->CallVoidMethod(_cvsView, drawtext, _cvs, (int) color.a, (int) color.r,
                                     (int) color.g, (int) color.b,
                                     wid, ammo, ammo2, pos.x, pos.y, size);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawOTH(Vec2 start, int num) {
        if (isValid()) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawline = _env->GetMethodID(cvsClass, "DrawOTH",
                                                   "(Landroid/graphics/Canvas;FF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawline != nullptr) {
                _env->CallVoidMethod(_cvsView, drawline, _cvs, start.x, start.y);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawOTH2(Vec2 start, int num, float width, float height) {
        if (isValid()) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawline = _env->GetMethodID(cvsClass, "DrawOTH2",
                                                   "(Landroid/graphics/Canvas;IFFFF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawline != nullptr) {
                _env->CallVoidMethod(_cvsView, drawline, _cvs, num, start.x, start.y, width, height);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawWeaponIcon(int wid, Vec2 pos) {
        if (isValid()) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawoth = _env->GetMethodID(cvsClass, "DrawWeaponIcon",
                                                  "(Landroid/graphics/Canvas;IFF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawoth != nullptr) {
                _env->CallVoidMethod(_cvsView, drawoth, _cvs, wid, pos.x, pos.y);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawUserID(Color color, const char *txt, Vec2 pos, float size) {
        if (isValid() && txt != nullptr) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawtext = _env->GetMethodID(cvsClass, "DrawUserID",
                                                   "(Landroid/graphics/Canvas;IIIILjava/lang/String;FFF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawtext != nullptr) {
                jstring s = _env->NewStringUTF(txt);
                _env->CallVoidMethod(_cvsView, drawtext, _cvs, (int) color.a, (int) color.r,
                                     (int) color.g, (int) color.b,
                                     s, pos.x, pos.y, size);
                _env->DeleteLocalRef(s);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawNation(Color color, const char *txt, Vec2 pos, float size) {
        if (isValid() && txt != nullptr) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawtext = _env->GetMethodID(cvsClass, "DrawNation",
                                                   "(Landroid/graphics/Canvas;IIIILjava/lang/String;IFFF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawtext != nullptr) {
                jstring s = _env->NewStringUTF(txt);
                _env->CallVoidMethod(_cvsView, drawtext, _cvs, (int) color.a, (int) color.r,
                                     (int) color.g, (int) color.b,
                                     s, 0, pos.x, pos.y, size);
                _env->DeleteLocalRef(s);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawTextBot(Color color, const char *txt, Vec2 pos, float size) {
        if (isValid() && txt != nullptr) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawtext = _env->GetMethodID(cvsClass, "DrawTextBot",
                                                   "(Landroid/graphics/Canvas;IIIILjava/lang/String;FFF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawtext != nullptr) {
                jstring s = _env->NewStringUTF(txt);
                _env->CallVoidMethod(_cvsView, drawtext, _cvs, (int) color.a, (int) color.r,
                                     (int) color.g, (int) color.b,
                                     s, pos.x, pos.y, size);
                _env->DeleteLocalRef(s);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawName1(Color color, const char *txt, int teamid, Vec2 pos, float size) {
        if (isValid() && txt != nullptr) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawtext = _env->GetMethodID(cvsClass, "DrawName1",
                                                   "(Landroid/graphics/Canvas;IIIILjava/lang/String;IFFF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawtext != nullptr) {
                jstring s = _env->NewStringUTF(txt);
                _env->CallVoidMethod(_cvsView, drawtext, _cvs, (int) color.a, (int) color.r,
                                     (int) color.g, (int) color.b,
                                     s, teamid, pos.x, pos.y, size);
                _env->DeleteLocalRef(s);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawDeadBoxItems(Color color, const char *txt, Vec2 pos, float size) {
        if (isValid() && txt != nullptr) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawtext = _env->GetMethodID(cvsClass, "DrawDeadBoxItems",
                                                   "(Landroid/graphics/Canvas;IIIILjava/lang/String;FFF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawtext != nullptr) {
                jstring s = _env->NewStringUTF(txt);
                _env->CallVoidMethod(_cvsView, drawtext, _cvs, (int) color.a, (int) color.r,
                                     (int) color.g, (int) color.b,
                                     s, pos.x, pos.y, size);
                _env->DeleteLocalRef(s);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawName2(Color color, const char *txt, int teamid, Vec2 pos, float size) {
        if (isValid() && txt != nullptr) {
            jclass cvsClass = _env->GetObjectClass(_cvsView);
            jmethodID drawtext = _env->GetMethodID(cvsClass, "DrawName2",
                                                   "(Landroid/graphics/Canvas;IIIILjava/lang/String;IFFF)V");
            if (_env->ExceptionCheck()) _env->ExceptionClear();
            if (drawtext != nullptr) {
                jstring s = _env->NewStringUTF(txt);
                _env->CallVoidMethod(_cvsView, drawtext, _cvs, (int) color.a, (int) color.r,
                                     (int) color.g, (int) color.b,
                                     s, teamid, pos.x, pos.y, size);
                _env->DeleteLocalRef(s);
            }
            _env->DeleteLocalRef(cvsClass);
        }
    }

    void DrawTextBold(Color color, const char *txt, Vec2 pos, float size) {
        DrawText(color, txt, pos, size);
    }

};

#endif //BETA_ESP_IMPORTANT_ESP_H

