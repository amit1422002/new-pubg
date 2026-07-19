#include <unistd.h>
#include <cstdlib>
#include <fcntl.h>
#include <dirent.h>
#include <pthread.h>
#include <fstream>
#include <cstring>
#include <ctime>
#include <malloc.h>
#include <iostream>
#include <fstream>
#include <sys/system_properties.h>
#include <ctime>
#include <cmath>
#include "TouchInput.hpp"

float ScrWidth,ScrHeight;
bool aimTouch;
/** Memory aimbot uses smooth touch drag (gyro stays in control). */
bool memoryAimAssist = false;
float aimSmooth = 18.f;
float aim_x,aim_y;
bool isAim = false;
bool ChargingPortLeft = false;
float aimingSpeed = 20.f;
float touchRange = 300.f;
float touchX = 650.f;
float touchY = 1400.f;
CameraView MinimalViewInfo{};
bool isDown = false;

static uint32_t sTouchAimRng = 0xB16B00B5u;

static inline uint32_t touchAimRand() {
    sTouchAimRng ^= sTouchAimRng << 13;
    sTouchAimRng ^= sTouchAimRng >> 17;
    sTouchAimRng ^= sTouchAimRng << 5;
    return sTouchAimRng;
}

static inline uint32_t touchAimSleepUs() {
    uint32_t base = (uint32_t) (aimSmooth * 1000.f);
    if (base < 8000u) base = 8000u;
    return base + (touchAimRand() % (base / 4u + 1u));
}

[[noreturn]] void *AimBotAuto(void *) {
    double tx = touchX, ty = touchY;
    int touchInitAttempts = 0;
    while (true) {
        if ((aimTouch || memoryAimAssist) && ScrWidth > 0 && ScrHeight > 0) {
            TouchInput::ensureTouchInput((int) ScrWidth, (int) ScrHeight);
            if (TouchInput::isTouchReady()) {
                break;
            }
        }
        if (++touchInitAttempts > 60) {
            break;
        }
        usleep(500000);
    }

    while (true) {
        float ScreenX = ScrWidth;
        float ScreenY = ScrHeight;
        float ScrXH = ScrWidth / 2.f;
        float ScrYH = ScrHeight / 2.f;
        float px = ScrXH;
        float py = ScrYH;
        float TargetX = 0;
        float TargetY = 0;
        float zm_x = 0, zm_y = 0;

        const bool assistOn = aimTouch || memoryAimAssist;
        if (!assistOn) {
            isAim = false;
        } else if (ScrWidth > 0 && ScrHeight > 0 && !TouchInput::isTouchReady()) {
            TouchInput::ensureTouchInput((int) ScrWidth, (int) ScrHeight);
        }

        float AimDs = sqrtf((px - aim_x) * (px - aim_x) + (py - aim_y) * (py - aim_y));
        zm_x = aim_x;
        zm_y = aim_y;
        if (aim_x <= 0 || aim_x >= ScreenX || aim_y <= 0 || aim_y >= ScreenY) {
            isAim = false;
        }
        //LOGI("ScrX %f, ScrY %f, isAIM %d, zm_x %f, zm_y %f, %f, %f", ScreenX, ScreenY, isAim, zm_x, zm_y, ty, tx);
        if (memoryAimAssist && !aimTouch && (touchAimRand() % 11u) == 0u) {
            if (isDown) {
                tx = touchX, ty = touchY;
                TouchInput::sendTouchUp();
                isDown = false;
            }
            usleep(touchAimSleepUs());
            continue;
        }

        if (isAim && TouchInput::isTouchReady()) {
            if (!isDown) {
                if (!ChargingPortLeft)
                    TouchInput::sendTouchMove((int) tx, (int) ty);
                else
                    TouchInput::sendTouchMove((int) (py * 2.f - (float) tx), (int) (px * 2.f - (float) ty));
                isDown = true;
            }
            float Aimspeace = aimingSpeed;
            if (AimDs < 1)
                Aimspeace = aimingSpeed / 0.09;
            else if (AimDs < 2)
                Aimspeace = aimingSpeed / 0.11;
            else if (AimDs < 3)
                Aimspeace = aimingSpeed / 0.12;
            else if (AimDs < 5)
                Aimspeace = aimingSpeed / 0.15;
            else if (AimDs < 10)
                Aimspeace = aimingSpeed / 0.25;
            else if (AimDs < 15)
                Aimspeace = aimingSpeed / 0.4;
            else if (AimDs < 20)
                Aimspeace = aimingSpeed / 0.5;
            else if (AimDs < 25)
                Aimspeace = aimingSpeed / 0.6;
            else if (AimDs < 30)
                Aimspeace = aimingSpeed / 0.7;
            else if (AimDs < 40)
                Aimspeace = aimingSpeed / 0.75;
            else if (AimDs < 50)
                Aimspeace = aimingSpeed / 0.8;
            else if (AimDs < 60)
                Aimspeace = aimingSpeed / 0.85;
            else if (AimDs < 70)
                Aimspeace = aimingSpeed / 0.9;
            else if (AimDs < 80)
                Aimspeace = aimingSpeed / 0.95;
            else if (AimDs < 90)
                Aimspeace = aimingSpeed / 1.0;
            else if (AimDs < 100)
                Aimspeace = aimingSpeed / 1.05;
            else if (AimDs < 150)
                Aimspeace = aimingSpeed / 1.25;
            else if (AimDs < 200)
                Aimspeace = aimingSpeed / 1.5;
            else
                Aimspeace = aimingSpeed / 1.55;

            if (memoryAimAssist && !aimTouch) {
                Aimspeace *= 1.15f + (touchAimRand() % 25u) * 0.01f;
            }

            if (memoryAimAssist && !aimTouch) {
                const float jitter = ((float) (touchAimRand() % 100u) / 100.f - 0.5f) * 0.12f;
                TargetX *= (1.f + jitter);
                TargetY *= (1.f - jitter * 0.6f);
            }

            //LOGI("AimSpeace : %f", Aimspeace);

            if (zm_x > ScrXH) {
                TargetX = -(ScrXH - zm_x);
                TargetX /= Aimspeace;
                if (TargetX + ScrXH > ScrXH * 2)
                    TargetX = 0;
            }
            if (zm_x < ScrXH) {
                TargetX = zm_x - ScrXH;
                TargetX /= Aimspeace;
                if (TargetX + ScrXH < 0)
                    TargetX = 0;
            }
            if (zm_y > ScrYH) {
                TargetY = -(ScrYH - zm_y);
                TargetY /= Aimspeace;
                if (TargetY + ScrYH > ScrYH * 2)
                    TargetY = 0;
            }
            if (zm_y < ScrYH) {
                TargetY = zm_y - ScrYH;
                TargetY /= Aimspeace;
                if (TargetY + ScrYH < 0)
                    TargetY = 0;
            }
            if (TargetY >= 35 || TargetX >= 35 || TargetY <= -35 || TargetX <= -35) {
                if (isDown) {
                    tx = touchX, ty = touchY;
                    TouchInput::sendTouchUp();
                    isDown = false;
                }
                usleep(touchAimSleepUs());
                continue;
            }
            //
            tx += TargetX;
            ty += TargetY;
          
			if(tx - (touchRange / 2) <= touchX - touchRange || tx + (touchRange / 2) >= touchX + touchRange || ty + (touchRange / 2) >= touchY + touchRange || ty - (touchRange / 2) <= touchY - touchRange){
                tx = touchX, ty = touchY;
                TouchInput::sendTouchUp();
				usleep(touchAimSleepUs());
			}
            if (!ChargingPortLeft)
                TouchInput::sendTouchMove( (int) tx, (int) ty);
            else
                TouchInput::sendTouchMove( py * 2 - (float) tx, px * 2 - (float) ty);
        } else {
            if (isDown) {
                tx = touchX, ty = touchY;
                TouchInput::sendTouchUp();
                isDown = false;
            }
        }
        usleep(touchAimSleepUs());
    }
}

