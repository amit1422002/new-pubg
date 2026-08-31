#ifndef BETA_ESP_IMPORTANT_HACKS_H
#define BETA_ESP_IMPORTANT_HACKS_H

#include "socket.h"
#include "Color.h"
#include "items.h"
//#include "rLogin/Login.h"
#include "Vector3.hpp"
extern bool DaddyXerr0r;  // declaration so other files can use it


Color clrEnemy, clrEdge, clrBox, clrAlert, clr, clrTeam, clrDist, clrHealth, clrText, grenadeColor, clrDistance;
float h, w, x, y, z, magic_number, mx, my, top, bottom, textsize, mScale, skelSize;
//Options options {1, -1, -1, -1,1, 1,200,300};
Options options{1, -1, -1, 3, false, false, 1, false, 200, 200, 200, 19, 19, -1, false};
OtherFeature otherFeature{false, false, false, false};

int botCount, playerCount;
Response response;
Request request;
char extra[30];
char text[100];
int hCounter = 50;

Color colorByDistance (int distance, int alpha) {
    Color clrDistance;
    if (distance < 600)
        clrDistance = Color::Yellow(255);

    if (distance < 300)
        clrDistance = Color::Orange(255);

    if (distance < 150)
        clrDistance = Color::Red(255);

    return clrDistance;
}

bool isOutsideSafeZone (Vec2 pos, Vec2 screen) {
    if (pos.y < 0) {
        return true;
    }
    if (pos.x > screen.x) {
        return true;
    }
    if (pos.y > screen.y) {
        return true;
    }
    return pos.x < 0;
}

std::string playerstatus (int GetEnemyState) {
    switch (GetEnemyState) {
        case 520:
        case 544:
        case 656:
        case 521:
        case 528:
        case 3145736:
            return "Aiming";
            break;
        default:
            return "";
            break;
    }
}

Vec2 calculatePosition (const Vec2 &center, float radius, float angleDegrees) {
    float angleRadians = angleDegrees * (M_PI / 180.0f); // Konversi derajat ke radian
    float x = center.x + radius * cos(angleRadians);
    float y = center.y + radius * sin(angleRadians);
    return Vec2(x, y);
}

float getDisplayAngle(Vec2 position, Vec2 screen) {
    float centerX = screen.x / 2;
    float centerY = screen.y / 2;
    return atan2(position.y - centerY, position.x - centerX) * (180.0 / M_PI);
}


bool colorPosCenter (float sWidth, float smMx, float sHeight, float posT, float eWidth, float emMx,
                     float eHeight, float posB) {
    if (sWidth >= smMx && sHeight >= posT && eWidth <= emMx && eHeight <= posB) {
        return true;
    }
    return false;
}

Vec2
pushToScreenBorder (const Vec2 &location, const Vec2 &screen, float offset, float scale = 2.0f) {
    Vec2 center(screen.x / 2, screen.y / 2);
    float angle = atan2(location.y - center.y, location.x - center.x) * (180.0f / M_PI);
    return calculatePosition(center, offset * scale, angle);
}

// TODO Draw Radar
void DrawRadar(ESP canvas, Vec2 Location, Vec2 Pos, float Size, Color clr, int TeamID) {

    float shiftX = 300.0f;
    float shiftY = 300.0f;

    // Screen Bounds ke andar rakhne ke liye clamp function
    float radarMinX = Pos.x - Size / 2 + shiftX;
    float radarMaxX = Pos.x + Size / 2 + shiftX;
    float radarMinY = Pos.y - Size / 2 + shiftY;
    float radarMaxY = Pos.y + Size / 2 + shiftY;

    Location.x = std::max(radarMinX, std::min(Location.x + shiftX, radarMaxX));
    Location.y = std::max(radarMinY, std::min(Location.y + shiftY, radarMaxY));

    // LocalPos ko draw karo (right shift applied)
    canvas.DrawTransRoundRect(Color::White(30), {shiftX - Size, shiftY - Size},
                               {shiftX + Size, shiftY + Size});

    canvas.DrawTransRoundRect(Color::White(255), {shiftX - Size/20, shiftY - Size/20},
                              {shiftX + Size/20, shiftY + Size/20});

    canvas.DrawFillCircle(Color(clr.r, clr.g, clr.b, 255), Location, Size / 10, 0.5);

    if (isPlayerName) {
        // TeamID ko draw karo
        canvas.DrawText(Color::White(255), std::to_string(TeamID).c_str(), Location, Size / 10);
    }
}






void DrawESP (ESP esp, int screenWidth, int screenHeight) {


                esp.DrawTextName(Color::Orange(255), "rayansyed77  ",
                                 Vec2(screenWidth / 7, screenHeight / 13.5), screenHeight / 40);

                //esp.DrawTextMode(Color::NavyBlue(255), "",
                //       Vec2(screenWidth / 9.3, screenHeight / 1.05), screenHeight / 45);

                const char *aimText = "";
                if (options.aimBullet == 0) {
                    aimText = "BULLET TRACK";
                } else if (options.openState == 0) {
                    aimText = "AIMBOT PREDICTION";
                } else if (options.aimT == 0) {
                    aimText = "TOUCH PREDICTION";
                } else if ((otherFeature.SmallCrosshair ||
                            otherFeature.WideView || otherFeature.Aimbot)) {
                    aimText = "MEMORY HACK";
                } else {
                    aimText = "ESP ONLY";
                }


                // esp.DrawTexture(Color::White(255), aimText,Vec2(screenWidth / 5, screenHeight / 1.09), screenHeight / 45);
                //esp.DrawTextMode2(Color::White(255), "", Vec2(screenWidth / 5, screenHeight / 1.13), screenHeight / 45);

                //esp.DrawTextBold(Color(255, 255, 255), credit.c_str(), Vec2(210, 80), 26);


                request.ScreenHeight = screenHeight;
                request.ScreenWidth = screenWidth;
                request.options = options;
                request.otherFeature = otherFeature;
                // InitMode every frame freezes UI (full actor+bone scan on draw thread).
                // DrawSyncMode = cached light path; InitMode only to discover/refresh.
                static int sEspFrame = 0;
                static int sLastPlayerCount = 0;
                ++sEspFrame;
                // Full actor discovery is the expensive path. Do not run it
                // every frame while the cache is empty; 4 Hz finds players
                // quickly without causing large CPU/memory-read spikes.
                const int discoverInterval = sLastPlayerCount <= 0 ? 15 : 30;
                const bool needDiscover =
                        sEspFrame == 1 || (sEspFrame % discoverInterval) == 0;
                request.Mode = needDiscover ? InitMode : DrawSyncMode;

                botCount = 0, playerCount = 0;
                send((void *) &request, sizeof(request));
                receive((void *) &response);
                if (response.Success) {
                    sLastPlayerCount = response.PlayerCount;
                } else if (needDiscover) {
                    sLastPlayerCount = 0;
                }
                float mScaleY = screenHeight / (float) 1080;
                mScale = screenHeight / (float) 1080;
                mScale = screenHeight / (float) 1080;
                skelSize = (mScale * 1.5f);
                float BoxSize = (mScaleY * 2.0f);
                textsize = screenHeight / 50;
                Vec2 screen(screenWidth, screenHeight);



                if (response.Success) {

                    int drawPlayerCount = response.PlayerCount;
                    if (drawPlayerCount < 0) {
                        drawPlayerCount = 0;
                    }
                    if (drawPlayerCount > maxplayerCount) {
                        drawPlayerCount = maxplayerCount;
                    }

                    for (int i = 0; i < drawPlayerCount; i++) {

                        PlayerData Player = response.Players[i];
                        x = Player.HeadLocation.x;
                        y = Player.HeadLocation.y;

                        sprintf(extra, "%0.0fM", Player.Distance);
                        float magic_number = (response.Players[i].Distance * response.fov);
                        // fov/distance 0 → giant shapes fill the screen; skip this player
                        if (magic_number < 0.01f) {
                            continue;
                        }
                        float namewidht = (screenWidth / 6) / magic_number;
                        float pp2 = namewidht / 2;
                        float mx = (screenWidth / 4) / magic_number;
                        float my = (screenWidth / 1.38) / magic_number;
                        float top = y - my + (screenWidth / 1.7) / magic_number;
                        float bottom = response.Players[i].Bone.lAn.y + my -
                                       (screenWidth / 1.7) / magic_number;
                        clrDist = colorByDistance((int) Player.Distance, 255);
                        clrAlert = _clrID((int) Player.TeamID, 80);
                        clrTeam = _clrID((int) Player.TeamID, 150);
                        clr = _clrID((int) Player.TeamID, 200);
                        Vec2 location(x, y);
                        float textsize = screenHeight / 50;

                        if (Player.isBot) {
                            botCount++;
                            clrEnemy = Color::White(255);
                            clrEdge = Color::White(80);
                            clrBox = Color::White(255);
                            clrText = Color::Black(255);
                        } else {
                            playerCount++;
                            clrEnemy = clrDist;
                            clrEdge = clrAlert;
                            clrBox = Color::Orange(255);
                            clrText = Color::White(255);
                        }

                        if (isRadar) {
                            DrawRadar(esp, Player.RadarLocation, request.radarPos,
                                      request.radarSize, clr, Player.TeamID);
                        }

                        if (response.Players[i].HeadLocation.z != 1) {
                            // On Screen
                            if (x > -50 && x < screenWidth + 50) {
                                float infoY = top - 110;
                                if (!response.Players[i].isVisible) {
                                    clrBox = Color::Red(255); 
                                    clrEdge = Color::Red(255);
                                } else {
                                    clrBox = Color::Green(255);
                                    clrEdge = Color::Green(255);
                                }


                                if (isSkeleton && Player.Bone.isBone) {
                                    float skelSize = (mScaleY * 2.0f);
                                    float headsize = (mScaleY * 7.0f);
                                    float distanceFromCamera = Player.Distance;
                                    float minHeadSize = (mScaleY * 2.0f);

                                    esp.DrawLine(clrBox, skelSize,
                                                 Vec2(response.Players[i].Bone.neck.x, response.Players[i].Bone.neck.y),
                                                 Vec2(response.Players[i].Bone.cheast.x, response.Players[i].Bone.cheast.y));
                                    esp.DrawLine(clrBox, skelSize,
                                                 Vec2(response.Players[i].Bone.cheast.x, response.Players[i].Bone.cheast.y),
                                                 Vec2(response.Players[i].Bone.pelvis.x, response.Players[i].Bone.pelvis.y));
                                    esp.DrawLine(clrBox, skelSize,
                                                 Vec2(response.Players[i].Bone.neck.x, response.Players[i].Bone.neck.y),
                                                 Vec2(response.Players[i].Bone.lSh.x, response.Players[i].Bone.lSh.y));
                                    esp.DrawLine(clrBox, skelSize,
                                                 Vec2(response.Players[i].Bone.neck.x, response.Players[i].Bone.neck.y),
                                                 Vec2(response.Players[i].Bone.rSh.x, response.Players[i].Bone.rSh.y));
                                    esp.DrawLine(clrBox, skelSize,
                                                 Vec2(response.Players[i].Bone.lSh.x, response.Players[i].Bone.lSh.y),
                                                 Vec2(response.Players[i].Bone.lElb.x, response.Players[i].Bone.lElb.y));
                                    esp.DrawFilledCircle(clrEdge,
                                                         Vec2(response.Players[i].Bone.lWr.x, response.Players[i].Bone.lWr.y),
                                                         screenHeight / 20 / magic_number);
                                    esp.DrawLine(clrBox, skelSize,
                                                 Vec2(response.Players[i].Bone.rSh.x, response.Players[i].Bone.rSh.y),
                                                 Vec2(response.Players[i].Bone.rElb.x, response.Players[i].Bone.rElb.y));
                                    esp.DrawFilledCircle(clrEdge,
                                                         Vec2(response.Players[i].Bone.rWr.x, response.Players[i].Bone.rWr.y),
                                                         screenHeight / 20 / magic_number);
                                    esp.DrawLine(clrBox, skelSize,
                                                 Vec2(response.Players[i].Bone.lElb.x, response.Players[i].Bone.lElb.y),
                                                 Vec2(response.Players[i].Bone.lWr.x, response.Players[i].Bone.lWr.y));
                                    esp.DrawLine(clrBox, skelSize,
                                                 Vec2(response.Players[i].Bone.rElb.x, response.Players[i].Bone.rElb.y),
                                                 Vec2(response.Players[i].Bone.rWr.x, response.Players[i].Bone.rWr.y));
                                    esp.DrawLine(clrBox, skelSize,
                                                 Vec2(response.Players[i].Bone.pelvis.x, response.Players[i].Bone.pelvis.y),
                                                 Vec2(response.Players[i].Bone.lTh.x, response.Players[i].Bone.lTh.y));
                                    esp.DrawLine(clrBox, skelSize,
                                                 Vec2(response.Players[i].Bone.pelvis.x, response.Players[i].Bone.pelvis.y),
                                                 Vec2(response.Players[i].Bone.rTh.x, response.Players[i].Bone.rTh.y));
                                    esp.DrawLine(clrBox, skelSize,
                                                 Vec2(response.Players[i].Bone.lTh.x, response.Players[i].Bone.lTh.y),
                                                 Vec2(response.Players[i].Bone.lKn.x, response.Players[i].Bone.lKn.y));
                                    esp.DrawLine(clrBox, skelSize,
                                                 Vec2(response.Players[i].Bone.rTh.x, response.Players[i].Bone.rTh.y),
                                                 Vec2(response.Players[i].Bone.rKn.x, response.Players[i].Bone.rKn.y));
                                    esp.DrawFilledCircle(clrEdge,
                                                         Vec2(response.Players[i].Bone.lAn.x, response.Players[i].Bone.lAn.y),
                                                         screenHeight / 20 / magic_number);
                                    esp.DrawLine(clrBox, skelSize,
                                                 Vec2(response.Players[i].Bone.lKn.x, response.Players[i].Bone.lKn.y),
                                                 Vec2(response.Players[i].Bone.lAn.x, response.Players[i].Bone.lAn.y));
                                    esp.DrawLine(clrBox, skelSize,
                                                 Vec2(response.Players[i].Bone.rKn.x, response.Players[i].Bone.rKn.y),
                                                 Vec2(response.Players[i].Bone.rAn.x, response.Players[i].Bone.rAn.y));
                                    esp.DrawFilledCircle(clrEdge,
                                                         Vec2(response.Players[i].Bone.rAn.x, response.Players[i].Bone.rAn.y),
                                                         screenHeight / 20 / magic_number);
                                }

                                // Player Box
                                if (isPlayerBox) {
                                esp.DrawLine(clrBox, BoxSize,
                                             Vec2(x + pp2, top),
                                             Vec2(x + namewidht, top));
                                esp.DrawLine(clrBox, BoxSize,
                                             Vec2(x - pp2, top),
                                             Vec2(x - namewidht, top));
                                esp.DrawLine(clrBox, BoxSize,
                                             Vec2(x + namewidht, top),
                                             Vec2(x + namewidht, top + pp2));
                                esp.DrawLine(clrBox, BoxSize,
                                             Vec2(x - namewidht, top),
                                             Vec2(x - namewidht, top + pp2));
                                // bottom
                                esp.DrawLine(clrBox, BoxSize,
                                             Vec2(x + pp2, bottom),
                                             Vec2(x + namewidht, bottom));
                                esp.DrawLine(clrBox, BoxSize,
                                             Vec2(x - pp2, bottom),
                                             Vec2(x - namewidht, bottom));
                                esp.DrawLine(clrBox, BoxSize,
                                             Vec2(x - namewidht, bottom),
                                             Vec2(x - namewidht, bottom - pp2));
                                esp.DrawLine(clrBox, BoxSize,
                                             Vec2(x + namewidht, bottom),
                                             Vec2(x + namewidht, bottom - pp2));

                                }

                                if (isPlayerLine){
                                esp.DrawLine(clrBox, screenHeight / 500,
                                             Vec2(screenWidth / 2, screenHeight / 12),
                                             Vec2(x, top - screenHeight / 32));
                                }

                           

// Sabse pehle Visibility check karein taake niche drawing mein sahi color use ho



float healthLength = screenWidth / 24;
float healthHeight = mScale * (screenHeight / 10);
float healthWidth = screenWidth / 200;
float healthX = x + mx - (screenHeight / 50) / magic_number;
float distance = response.Players[i].Distance;

if (healthLength < mx)
    healthLength = mx;

float hp = response.Players[i].Health;
if (hp > 100.0f) hp = 100.0f;
if (hp < 0.0f) hp = 0.0f;

if (hp < 25.0f)
    clrHealth = Color(255, 50, 50, 230);
else if (hp < 50.0f)
    clrHealth = Color(255, 165, 0, 230);
else if (hp < 75.0f)
    clrHealth = Color(255, 235, 50, 230);
else
    clrHealth = Color(46, 213, 115, 230);

if (hp > 0.0f) {
    if (isPlayerHealth) {
        float barHalfWidth = screenWidth / 28.0f;
        float barLeft = x - barHalfWidth;
        float barRight = x + barHalfWidth;
        float barTop = infoY + 45.0f;
        float barBottom = infoY + 53.0f;
        float currentHpRight = barLeft + (barRight - barLeft) * (hp / 100.0f);

        // Dark background representing total max HP
        esp.DrawFilledRect(
            Color(20, 20, 20, 190),
            Vec2(barLeft, barTop),
            Vec2(barRight, barBottom)
        );

        // Dynamic colored bar representing remaining HP
        if (hp > 0.5f) {
            esp.DrawFilledRect(
                clrHealth,
                Vec2(barLeft, barTop),
                Vec2(currentHpRight, barBottom)
            );
        }

        // Crisp border outline
        esp.DrawRect(
            Color(255, 255, 255, 220),
            1.0f,
            Vec2(barLeft, barTop),
            Vec2(barRight, barBottom)
        );
    }
}

if (isPlayerName && response.Players[i].isBot) {

    esp.DrawText(
        Color().White(255),
        "ROBOT",
        Vec2(x + 5.5, infoY + 80),
        textsize
    );

} else if (isPlayerName) {

    esp.DrawName(
        Color(255, 255, 255),
        response.Players[i].PlayerNameByte,
        response.Players[i].TeamID,
        Vec2(x + 5.5, infoY + 80),
        screenHeight / 53
    );
}

                                if (isPlayerTeamID) {

                                    // Name
                                    esp.DrawFilledName(Color(233, 105, 116),
                                                       Vec2(x - screenWidth / 25,
                                                            top - screenHeight / 18),
                                                       Vec2(x - screenWidth / 48.5,
                                                            top - screenHeight / 33.5));



                                    esp.DrawTeamID(Color(255, 255, 255),
                                                   response.Players[i].TeamID,
                                                   Vec2(x - screenWidth / 33.5,
                                                        top - screenHeight / 28),
                                                   screenHeight / 53);

                                }



                                //Nation

                                if (isPlayerNation) {
                                    if (response.Players[i].Health <= 0) {
                                        //null
                                    } else {
                                        if (response.Players[i].isBot) {
                                            //nul
                                            esp.DrawNation(Color(255, 255, 255, 255),
                                                           response.Players[i].PlayerNation,
                                                           Vec2(x - -10, top - -7), 28);
                                        } else {
                                            esp.DrawNation(Color(255, 255, 255, 255),
                                                           response.Players[i].PlayerNation,
                                                           Vec2(x - -10, top - -7), 28);
                                        }
                                    }
                                }

                                // UID
                                if (isPlayerUID) {

                                    esp.DrawUserID(Color().Orange(255),
                                                   response.Players[i].PlayerUID, Vec2(
                                                    response.Players[i].HeadLocation.x - 25,
                                                    top - screenHeight / 15), screenHeight / 60);
                                }


                                //Player Head
                                if (isPlayerHead){
                                esp.DrawFilledCircle(clrEdge,
                                                     Vec2(response.Players[i].HeadLocation.x,
                                                          response.Players[i].HeadLocation.y),
                                                     screenHeight / 12 / magic_number);
                                }

                                /*//Player Names
                                if (response.Players[i].isBot) {
                                    sprintf(extra, "B O T");
                                    esp.DrawText(Color(255, 255, 255), extra,
                                                 Vec2(x, top - 12),
                                                 textsize);
                                } else {
                                    esp.DrawName(Color().White(255),
                                                 response.Players[i].PlayerNameByte,
                                                 response.Players[i].TeamID,
                                                 Vec2(response.Players[i].HeadLocation.x,
                                                      top - 12),
                                                 textsize);
                                }*/

                                if (isPlayerDistance) {
                                sprintf(extra, "%0.0f M", response.Players[i].Distance);
                                esp.DrawText(Color(247, 175, 63, 255), extra,
                                             Vec2(x, bottom + screenHeight / 45),
                                             textsize);


                                }


                                // weapon text only
                                if (isPlayerWeapon && response.Players[i].Weapon.isWeapon) {
                                    esp.DrawWeapon(Color(247, 244, 200),
                                                   response.Players[i].Weapon.id,
                                                   response.Players[i].Weapon.ammo,
                                                   response.Players[i].Weapon.ammo,
                                                   Vec2(x, bottom + screenHeight / 23), textsize);
                                }


                                // if (/*isPlayerWeaponIcon && */response.Players[i].Weapon.isWeapon) {
                                //     esp.DrawWeaponIcon(response.Players[i].Weapon.id,
                                //                        Vec2(x - 45, top - 60));
                                // }

                                if (Player.isVisible) {
                                    if (playerstatus(Player.StatusPlayer) == "Aiming") {
                                        esp.DrawTexture(Color::Yellow(255),
                                                        " ⚠\uFE0F Player Aiming at you ⚠\uFE0F",
                                                        Vec2(screenWidth / 2, screenHeight / 4.3),
                                                        screenHeight / 30);
                                    }
                                }


                            } //OnScreen

                            if (is360Alert) {
                                // 360 alert
                                if (response.Players[i].HeadLocation.z == 1.0f) {

                                    if (x > screenWidth - screenWidth / 12)
                                        x = screenWidth - screenWidth / 120;
                                    else if (x < screenWidth / 120)
                                        x = screenWidth / 12;

                                    if (y < screenHeight / 1) {
                                        esp.DrawRect(Color(255, 255, 255), 2,
                                                     Vec2(screenWidth - x - 100, screenHeight - 48),
                                                     Vec2(screenWidth - x + 100, screenHeight + 2));
                                        esp.DrawFilledRect(Color(255, 0, 0, 140),
                                                           Vec2(screenWidth - x - 100,
                                                                screenHeight - 48),
                                                           Vec2(screenWidth - x + 100,
                                                                screenHeight + 2));
                                        sprintf(extra, "%0.0f m", response.Players[i].Distance);
                                        esp.DrawText(Color(255, 255, 255, 255), extra,
                                                     Vec2(screenWidth - x, screenHeight - 20),
                                                     textsize);
                                    } else {
                                        esp.DrawRect(Color(255, 255, 255), 2,
                                                     Vec2(screenWidth - x - 100, 48),
                                                     Vec2(screenWidth - x + 100, -2));
                                        esp.DrawFilledRect(Color(255, 0, 0, 140),
                                                           Vec2(screenWidth - x - 100, 48),
                                                           Vec2(screenWidth - x + 100, -2));
                                        sprintf(extra, "%0.0f m", response.Players[i].Distance);
                                        esp.DrawText(Color(255, 255, 255, 255), extra,
                                                     Vec2(screenWidth - x, 25), textsize);
                                    }
                                } else if (x < -screenWidth / 10 ||
                                           x > screenWidth + screenWidth / 10) {

                                    if (y > screenHeight - screenHeight / 12)
                                        y = screenHeight - screenHeight / 120;
                                    else if (y < screenHeight / 120)
                                        y = screenHeight / 12;

                                    if (x > screenWidth / 2) {
                                        esp.DrawRect(Color(255, 255, 255), 2,
                                                     Vec2(screenWidth - 88, y - 35),
                                                     Vec2(screenWidth + 2, y + 35));
                                        esp.DrawFilledRect(Color(255, 0, 0, 140),
                                                           Vec2(screenWidth - 88, y - 35),
                                                           Vec2(screenWidth + 2, y + 35));
                                        sprintf(extra, "%0.0f m", response.Players[i].Distance);
                                        esp.DrawText(Color(255, 255, 255, 255), extra,
                                                     Vec2(screenWidth - screenWidth / 80, y + 10),
                                                     textsize);
                                    } else {
                                        esp.DrawRect(Color(255, 255, 255), 2,
                                                     Vec2(0 + 88, y - 35), Vec2(0 - 2, y + 35));
                                        esp.DrawFilledRect(Color(255, 0, 0, 140),
                                                           Vec2(0 + 88, y - 35),
                                                           Vec2(0 - 2, y + 35));
                                        sprintf(extra, "%0.0f m", response.Players[i].Distance);
                                        esp.DrawText(Color(255, 255, 255, 255), extra,
                                                     Vec2(screenWidth / 80, y + 10), textsize);
                                    }
                                } else if (y < -screenHeight / 10 ||
                                           y > screenHeight + screenHeight / 10) {

                                    if (x > screenWidth - screenWidth / 12)
                                        x = screenWidth - screenWidth / 120;
                                    else if (x < screenWidth / 120)
                                        x = screenWidth / 12;

                                    if (y > screenHeight / 2.5) {
                                        esp.DrawRect(Color(255, 255, 255), 2,
                                                     Vec2(x - 100, screenHeight - 48), Vec2(x + 100,
                                                                                            screenHeight +
                                                                                            2));
                                        esp.DrawFilledRect(Color(255, 0, 0, 140),
                                                           Vec2(x - 100, screenHeight - 48),
                                                           Vec2(x + 100, screenHeight + 2));
                                        sprintf(extra, "%0.0f m", response.Players[i].Distance);
                                        esp.DrawText(Color(255, 255, 255, 255), extra,
                                                     Vec2(x, screenHeight - 20), textsize);
                                    } else {
                                        esp.DrawRect(Color(255, 255, 255), 2,
                                                     Vec2(x - 100, 48), Vec2(x + 100, -2));
                                        esp.DrawFilledRect(Color(255, 0, 0, 140),
                                                           Vec2(x - 100, 48), Vec2(x + 100, -2));
                                        sprintf(extra, "%0.0f m", response.Players[i].Distance);
                                        esp.DrawText(Color(255, 255, 255, 255), extra,
                                                     Vec2(x, 25), textsize);
                                    }

                                }

                                if (isOutsideSafeZone(location, screen)) {
                                    // Triangle ko screen ke border pe push karo
                                    Vec2 hintDotRenderPos = pushToScreenBorder(location, screen,
                                                                               (mScaleY * 100) / 2,
                                                                               5.0f);


                                    float angle = getDisplayAngle(hintDotRenderPos, screen);

                                    if (response.Players[i].isBot) {
                                        esp.DrawTriangle(Color::Green(255), hintDotRenderPos,
                                                         (mScaleY * 20), angle);
                                    } else {
                                        esp.DrawTriangle(Color::Red(255), hintDotRenderPos,
                                                         (mScaleY * 20), angle);
                                    }
                                }

                            }


                        } //Player.HeadLocation.z
                    } //response.PlayerCount


                   /* std::vector<Vec2> grenadeTrails;

                    for (int i = 0; i < response.GrenadeCount; i++) {
                        GrenadeData grenade = response.Grenade[i];
                        if (!isGrenadeWarning || grenade.Location.z == 1.0f) {
                            continue;
                        }

                        const char *grenadeTypeText;
                        switch (grenade.type) {
                            case 1:
                                grenadeColor = Color::Red(255);
                                grenadeTypeText = "Grenade";
                                break;
                            case 2:
                                grenadeColor = Color::Orange(255);
                                grenadeTypeText = "Molotov";
                                break;
                            case 3:
                                grenadeColor = Color::Yellow(255);
                                grenadeTypeText = "Stun";
                                break;
                            default:
                                grenadeColor = Color::White(255);
                                grenadeTypeText = "Smoke";
                        }

                        sprintf(extra, "%s (%0.0f m)", grenadeTypeText, grenade.Distance);
                        sprintf(text, "Throwable %s (%0.0f m)", grenadeTypeText, grenade.Distance);

                        esp.DrawLine(grenadeColor, skelSize,
                                     Vec2(screenWidth / 2, screenHeight / 2.72),
                                     Vec2(grenade.Location.x, grenade.Location.y));

                        // Define Base Circle Radii (meters)
                        float baseRadii[3] = {60.0f, 40.0f, 20.0f};
                        Color circleColors[3] = {Color::Yellow(255), Color::Orange(255), Color::Red(255)};

                        int centerX = grenade.Location.x;
                        int centerY = grenade.Location.y;

                        // Perspective Scaling Factor (far = small, near = big)
                        float distanceFactor = std::max(1.0f, grenade.Distance / 60.0f);

                        for (int j = 0; j < 3; j++) {
                            // Scale Radius with Distance
                            float radius = (baseRadii[j] * 10) / distanceFactor;
                            Color currentColor = circleColors[j];

                            for (float angle = 0; angle <= 360; angle += 5) {
                                float radian = angle * (3.14159265f / 180.0f);
                                float x = centerX + radius * cos(radian);
                                float y = centerY + ((radius * 0.3f) / distanceFactor) * sin(radian); // Ground Projection Effect

                                esp.DrawLine(currentColor, skelSize, Vec2(centerX, centerY), Vec2(x, y));
                            }
                        }

                        // Store Grenade Path (Smooth Continuous Line)
                        grenadeTrails.push_back(Vec2(centerX, centerY));
                        if (grenadeTrails.size() > 30) {
                            grenadeTrails.erase(grenadeTrails.begin());
                        }

                        // Draw Grenade Path
                        for (size_t k = 1; k < grenadeTrails.size(); k++) {
                            esp.DrawLine(Color::White(255), skelSize, grenadeTrails[k - 1], grenadeTrails[k]);
                        }

                        esp.DrawOTH2(Vec2(screenWidth / 2 - screenHeight / 4, screenHeight / 3.2), 4, 600.0f, 70.0f);
                        esp.DrawText(grenadeColor, extra, Vec2(grenade.Location.x, grenade.Location.y + (screenHeight / 50)), textsize);
                        esp.DrawTexture(Color::White(255), text, Vec2(screenWidth / 2 + screenHeight / 300, screenHeight / 2.85), screenHeight / 40);
                    } */

                    for (int i = 0; i < response.GrenadeCount; i++) {
                        GrenadeData grenade = response.Grenade[i];
                        if (!isGrenadeWarning || grenade.Location.z == 1.0f) {
                            continue;
                        }

                        const char *grenadeTypeText;
                        switch (grenade.type) {
                            case 1:
                                grenadeColor = Color::Red(255);
                                grenadeTypeText = "Grenade";
                                break;
                            case 2:
                                grenadeColor = Color::Orange(255);
                                grenadeTypeText = "Molotov";
                                break;
                            case 3:
                                grenadeColor = Color::Yellow(255);
                                grenadeTypeText = "Stun";
                                break;
                            default:
                                grenadeColor = Color::White(255);
                                grenadeTypeText = "Smoke";
                        }

                        int WARNING = 4;
                        sprintf(extra, "%s (%0.0f m)", grenadeTypeText, grenade.Distance);
                        sprintf(text, "Throwable %s (%0.0f m)", grenadeTypeText, grenade.Distance);

                        esp.DrawLine(grenadeColor, skelSize,
                                     Vec2(screenWidth / 2, screenHeight / 2.72),
                                     Vec2(grenade.Location.x, grenade.Location.y));

                        int radius = 15; // نصف قطر الدائرة
                        int centerX = grenade.Location.x; // إحداثيات المركز في المحور X
                        int centerY = grenade.Location.y; // إحداثيات المركز في المحور Y

                        // رسم الدائرة
                        for (float angle = 0; angle <= 360; angle += 1) {
                            float radian = angle * (3.14159265f / 180.0f); // تحويل الزاوية إلى راديان
                            int x = centerX + radius * cos(radian); // إحداثيات X للنقطة على محيط الدائرة
                            int y = centerY + radius * sin(radian); // إحداثيات Y للنقطة على محيط الدائرة

                            esp.DrawLine(grenadeColor, skelSize, Vec2(centerX, centerY), Vec2(x, y)); // رسم خط من المركز إلى النقطة على المحيط
                        }

                        // باقي الكود
                        esp.DrawOTH2(Vec2(screenWidth / 2 - screenHeight / 4, screenHeight / 3.2), 4, 600.0f, 70.0f);
                        esp.DrawText(grenadeColor, extra, Vec2(grenade.Location.x, grenade.Location.y + (screenHeight / 50)), textsize);
                        esp.DrawTexture(Color::White(255), text, Vec2(screenWidth / 2 + screenHeight / 300, screenHeight / 2.85), screenHeight / 40);
                    }







                    for (int i = 0; i < response.VehicleCount; i++) {
                        if (isVehicles) {
                            VehicleData vehicle = response.Vehicles[i];
                            if (vehicle.Location.z != 1.0f) {
                                esp.DrawVehicles(vehicle.VehicleName, vehicle.Distance,
                                                 vehicle.Health, vehicle.Fuel,
                                                 Vec2(vehicle.Location.x, vehicle.Location.y),
                                                 screenHeight / 47);
                            }
                        }
                    } //response.VehicleCount

                    for (int i = 0; i < response.ItemsCount; i++) {
                        if (isItems) {
                            ItemData currentItem = response.Items[i];
                            if (currentItem.Location.z != 1.0f) {
                                esp.DrawItems(currentItem.ItemName, currentItem.Distance,
                                              Vec2(currentItem.Location.x, currentItem.Location.y),
                                              screenHeight / 50);
                            }
                        }
                    } //response.ItemsCount

                } //response.Success


                if (botCount + playerCount > 0) {
                    sprintf(extra, "ENEMIES: %d", botCount + playerCount);
                    float rectWidth = esp.measureTextWidth(extra, screenHeight / 40) + 40;
                    float rectHeight = screenHeight / 25;
                    float centerX = screenWidth / 2;
                    float centerY = screenHeight / 12;

                    // Draw stylized background box
                    esp.DrawFilledRoundRect(Color(0, 181, 229, 100), Vec2(centerX - rectWidth/2, centerY - rectHeight/2), Vec2(centerX + rectWidth/2, centerY + rectHeight/2));
                    esp.DrawRect(Color(0, 181, 229, 255), 2.0f, Vec2(centerX - rectWidth/2, centerY - rectHeight/2), Vec2(centerX + rectWidth/2, centerY + rectHeight/2));

                    // Draw bold enemy count text
                    esp.DrawText(Color::White(255), extra, Vec2(centerX, centerY + rectHeight/10), screenHeight / 40);
                } else {
                    sprintf(extra, "HUNTING...");
                    float rectWidth = esp.measureTextWidth(extra, screenHeight / 45) + 30;
                    float rectHeight = screenHeight / 30;
                    float centerX = screenWidth / 2;
                    float centerY = screenHeight / 12;

                    esp.DrawFilledRoundRect(Color(50, 50, 50, 100), Vec2(centerX - rectWidth/2, centerY - rectHeight/2), Vec2(centerX + rectWidth/2, centerY + rectHeight/2));
                    esp.DrawText(Color(0, 255, 0, 200), extra, Vec2(centerX, centerY + rectHeight/10), screenHeight / 45);
                }


                if (options.tracingStatus) {
                    float py = screenHeight / 2;
                    float px = screenWidth / 2;
                    esp.DrawFilledRect(Color::Green(50),
                                       Vec2(options.touchY - options.touchSize / 2,
                                            py * 2 - options.touchX + options.touchSize / 2),
                                       Vec2(options.touchY + options.touchSize / 2,
                                            py * 2 - options.touchX - options.touchSize / 2));
                }

                if (options.openState == 0 || options.aimBullet == 0 || options.aimT == 0) {
                    const Color textColor = (options.openState == 0) ? Color::Red(255) : (
                            options.aimT == 0 ? Color::Blue(255) : Color::Green(255));
                    esp.DrawCircle(textColor, Vec2(screenWidth / 2, screenHeight / 2),
                                   options.aimingRange, 1.5);
                }

                if (isLootItems) {
                    for (int i = 0; i < response.BoxItemsCount; i++) {
                        if (response.BoxItems[i].Location.z != 1.0f) {
                            BoxItemData *boxData = &response.BoxItems[i];
                            char *itemname;
                            int BoxCount = 0;
                            for (int ij = 0; ij < boxData->itemCount; ij++) {
                                if (GetBox((int) boxData->itemID[ij], &itemname)) {
                                    BoxCount++;
                                    esp.DrawDeadBoxItems(Color(), itemname,
                                                         Vec2(boxData->Location.x,
                                                              boxData->Location.y -
                                                              (float) BoxCount *
                                                              (screenHeight / 50)),
                                                         textsize);
                                }
                            }
                        }
                    }
                }



} //OnDraw

#endif // BETA_ESP_IMPORTANT_HACKS_H
