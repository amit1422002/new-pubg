#ifndef STRUCT_OFFSETS_H
#define STRUCT_OFFSETS_H

#include <cstdint>

// BGMI SDK: bgmi sdk/com.pubg.imobile/Offsets.hpp + ST_*_classes.hpp
// typeeee mapping (Main64 resolveGamePidOnce):
//   1 = Global/KR/TW, 2 = VNG, 3 = BGMI (com.pubg.imobile)

namespace OffsetsGame {
    uintptr_t GNames = 0;
    uintptr_t GWorld = 0;
    uintptr_t GWorldExternal = 0; // UEExternalPointers::WorldExternal — for out-of-process
    uintptr_t GEngine = 0;
    uintptr_t GUObjectArray = 0;
    uintptr_t GetActorArray = 0;

    void GameOffset(int a) {
        if (a == 1) {
            GNames = 0xE776400;
            GWorld = 0xECF4540;
            GWorldExternal = 0;
            GEngine = 0;
            GUObjectArray = 0;
            GetActorArray = 0;
        }
        else if (a == 2) {
            GNames = 0xe833500;
            GWorld = 0xEDB1640;
            GWorldExternal = 0;
            GEngine = 0;
            GUObjectArray = 0;
            GetActorArray = 0;
        }
        else if (a == 3) {
            // Latest BGMI dump (UEPointers) — user confirmed
            GNames = 0xE40BB20;
            GWorld = 0xE40B678;
            GWorldExternal = 0xCB821B0; // UEExternalPointers::WorldExternal
            GEngine = 0xE993C00;
            GUObjectArray = 0xE6D36F0;
            GetActorArray = 0xA45A314;
        }
    }
}

namespace OffsetsAll64 {

    uintptr_t PlayerController = 0;
    uintptr_t NetDriver = 0;
    uintptr_t PersistentLevel = 0;
    uintptr_t ServerConnection = 0;
    uintptr_t OwningGameInstance = 0; // UWorld → UGameInstance
    uintptr_t LocalPlayers = 0;       // UGameInstance → TArray<ULocalPlayer*>
    uintptr_t EncryptedLocalPlayers = 0;
    uintptr_t UseEncryptLocalPlayerPtr = 0;
    uintptr_t GameState = 0;          // UWorld → AGameState
    uintptr_t PlayerArray = 0;        // AGameState → TArray<APlayerState*>
    uintptr_t LevelOwningWorld = 0;   // ULevel → UWorld (circular validate)
    uintptr_t LevelActorCluster = 0;
    uintptr_t ActorClusterActors = 0;
    uintptr_t LevelActorsLegacy = 0;
    uintptr_t PawnPrivate = 0;
    uintptr_t PawnController = 0;
    uintptr_t STPlayerController = 0;
    uintptr_t PlayerCameraManager = 0;
    uintptr_t AcknowledgedPawn = 0;
    uintptr_t Mesh = 0;
    uintptr_t SkeletalMeshComponent = 0;
    uintptr_t StaticMesh = 0;
    uintptr_t STExtraBaseCharacter = 0;
    uintptr_t CharacterMovement = 0;
    uintptr_t MovementCharacter = 0;
    uintptr_t RootComponent = 0;
    uintptr_t MasterPoseComponent = 0;
    uintptr_t Children = 0;
    uintptr_t Position = 0;
    uintptr_t Velocity = 0;
    uintptr_t Health = 0;
    uintptr_t TeamID = 0;
    uintptr_t PlayerName = 0;
    uintptr_t bDead = 0;
    uintptr_t bIsAI = 0;
    uintptr_t Role = 0;
    uintptr_t PlayerUID = 0;
    uintptr_t Nation = 0;
    uintptr_t CurrentStates = 0;

    uintptr_t WeaponId = 0;
    uintptr_t WeaponEntityComp = 0;
    uintptr_t CurrWeapon = 0;
    uintptr_t CurrentWeaponReplicated = 0;
    uintptr_t ShootWeaponEntityComp = 0;
    uintptr_t WeaponManagerComponent = 0;

    uintptr_t CurBulletNumInClip = 0;
    uintptr_t CurUseWeaponLogicSocketRep = 0;
    uintptr_t bIsWeaponFiring = 0;
    uintptr_t bIsGunADS = 0;

    uintptr_t ShootMode = 0;
    uintptr_t BulletFireSpeed = 0;
    uintptr_t ShootInterval = 0;
    uintptr_t BulletTrackDistanceFix = 0;

    uintptr_t AccessoriesVRecoilFactor = 0;
    uintptr_t AccessoriesHRecoilFactor = 0;
    uintptr_t AccessoriesRecoveryFactor = 0;
    uintptr_t GameDeviationFactor = 0;
    uintptr_t RecoilKickADS = 0;
    uintptr_t ExtraHitPerformScale = 0;
    uintptr_t AnimationKick = 0;

    uintptr_t SRecoilInfo = 0;
    uintptr_t POV = 0;
    uintptr_t MinimalViewInfo = 0;
    uintptr_t CameraCache = 0;

    uintptr_t FieldOfView = 0;
    uintptr_t ViewPitchMin = 0;
    uintptr_t ViewPitchMax = 0;
    uintptr_t ViewYawMin = 0;
    uintptr_t ViewYawMax = 0;

    uintptr_t LastRenderTime = 0;
    uintptr_t CurrentVehicle = 0;
    uintptr_t VehicleCommon = 0;

    uintptr_t VHealth = 0;
    uintptr_t VHealthMax = 0;
    uintptr_t VFuel = 0;
    uintptr_t VFuelMax = 0;

    uintptr_t GameReplayType = 0;
    uintptr_t MaxBulletImpactFXClampDistance = 0;
    uintptr_t CustomTimeDilation = 0;

    uintptr_t bIsEngineStarted = 0;
    uintptr_t ExtraBoostFactor = 0;

    uintptr_t NearDeathBreath = 0;
    uintptr_t NearDeathComponent = 0;
    uintptr_t BreathMax = 0;

    uintptr_t ReplicatedMovement = 0;
    uintptr_t Controller = 0;
    uintptr_t ControlRotation = 0;
    uintptr_t AimControlRotationAdditive = 0;

    uintptr_t FixAttachInfoList = 0;
    uintptr_t UploadInterval = 0;
    uintptr_t PickUpDataList = 0;
    uintptr_t SwitchWeaponSpeedScale = 0;
    uintptr_t ThirdPersonCameraComponent = 0;
    uintptr_t DrawShootLineTime = 0;

    void GameType64(int a) {

        // default clear
        if (a == 0) return;

        if (a == 1 || a == 2) {

            PlayerController = 0x30;
            NetDriver = 0x38;
            PersistentLevel = 0x30;
            ServerConnection = 0x78;
            OwningGameInstance = 0x470;
            LocalPlayers = 0x48;
            EncryptedLocalPlayers = 0x38;
            UseEncryptLocalPlayerPtr = 0x80;
            GameState = 0x428;
            PlayerArray = 0x4C8;
            LevelOwningWorld = 0xC0;
            LevelActorCluster = 0xE0;
            ActorClusterActors = 0x28;
            LevelActorsLegacy = 0xA0;
            PawnPrivate = 0x528;
            PawnController = 0x4E8;
            STPlayerController = 0x4980;
            PlayerCameraManager = 0x548;
            AcknowledgedPawn = 0x528;

            Mesh = 0x510;
            SkeletalMeshComponent = 0x510;
            StaticMesh = 0x9a8;

            STExtraBaseCharacter = 0x28b0;
            CharacterMovement = 0x518;
            MovementCharacter = 0x518;
            RootComponent = 0x208;
            MasterPoseComponent = 0x9a0;

            Children = 0x1f8;
            Position = 0x1e4;
            Velocity = 0x18c;

            Health = 0xe60;
            TeamID = 0x998;
            PlayerName = 0x960;
            bDead = 0xe7c;
            bIsAI = 0xa59;
            Role = 0x1a8;

            PlayerUID = 0x988;
            Nation = 0x970;
            CurrentStates = 0x1058;

            WeaponId = 0x1e0;
            WeaponEntityComp = 0x1360;
            CurrWeapon = 0x5c8;
            CurrentWeaponReplicated = 0x5c8;
            ShootWeaponEntityComp = 0x1360;
            WeaponManagerComponent = 0x25c8;

            CurBulletNumInClip = 0x8;
            CurUseWeaponLogicSocketRep = 0x320;

            bIsWeaponFiring = 0x1818;
            bIsGunADS = 0x1134;

            ShootMode = 0x10d9;
            BulletFireSpeed = 0x560;
            ShootInterval = 0x5a0;
            BulletTrackDistanceFix = 0x930;

            AccessoriesVRecoilFactor = 0xbc8;
            AccessoriesHRecoilFactor = 0xbd0;
            AccessoriesRecoveryFactor = 0xbcc;

            GameDeviationFactor = 0xc2c;
            RecoilKickADS = 0xcf0;
            ExtraHitPerformScale = 0xcf8;
            AnimationKick = 0xcf4;

            SRecoilInfo = 0xb60;
            POV = 0x10;
            MinimalViewInfo = 0x10;
            CameraCache = 0x520;

            FieldOfView = 0x39c;
            ViewPitchMin = 0x1d4c;
            ViewPitchMax = 0x1d50;
            ViewYawMin = 0x1d54;
            ViewYawMax = 0x1d58;

            LastRenderTime = 0x488;
            CurrentVehicle = 0xeb0;
            VehicleCommon = 0xc00;

            VHealth = 0x354;
            VHealthMax = 0x350;
            VFuel = 0x43c;
            VFuelMax = 0x438;

            GameReplayType = 0xa1c;
            MaxBulletImpactFXClampDistance = 0xce0;
            CustomTimeDilation = 0xe4;

            bIsEngineStarted = 0xbc8;
            ExtraBoostFactor = 0x201c;

            NearDeathBreath = 0x1c10;
            NearDeathComponent = 0x1bf8;
            BreathMax = 0x1cc;

            ReplicatedMovement = 0x110;
            Controller = 0x4e8;
            ControlRotation = 0x4e0;
            AimControlRotationAdditive = 0x1a38;

            FixAttachInfoList = 0x200;
            UploadInterval = 0x1e0;
            PickUpDataList = 0x968;
            SwitchWeaponSpeedScale = 0x2bfc;
            ThirdPersonCameraComponent = 0x1d50;

            DrawShootLineTime = 0x13c;
        }

        // BGMI — offsets from bgmi sdk ST_*_classes.hpp
        if (a == 3) {

            PlayerController = 0x30;
            NetDriver = 0x38;
            PersistentLevel = 0x30;
            ServerConnection = 0x78;
            // DeltaForce/blackbox_raw BgmiOffsets.hpp local chain
            OwningGameInstance = 0x470;
            LocalPlayers = 0x48;
            EncryptedLocalPlayers = 0x38;
            UseEncryptLocalPlayerPtr = 0x80;
            GameState = 0x428;
            PlayerArray = 0x4C8;
            LevelOwningWorld = 0xC0;
            LevelActorCluster = 0xE0;
            ActorClusterActors = 0x28;
            LevelActorsLegacy = 0xA0;
            PawnPrivate = 0x528;
            PawnController = 0x4E8;
            STPlayerController = 0x4B18;
            PlayerCameraManager = 0x548;
            AcknowledgedPawn = 0x528;

            Mesh = 0x510;
            SkeletalMeshComponent = 0x510;
            StaticMesh = 0x9a8;

            STExtraBaseCharacter = 0x28b0;
            CharacterMovement = 0x518;
            MovementCharacter = 0x518;
            RootComponent = 0x208;
            MasterPoseComponent = 0x9a0;

            Children = 0x1f8;
            Position = 0x1e4;
            Velocity = 0x2c0;

            Health = 0xe60;
            TeamID = 0x998;
            PlayerName = 0x960;
            bDead = 0xe7c;
            bIsAI = 0xa40;
            Role = 0x1a8;

            PlayerUID = 0x988;
            Nation = 0x970;
            CurrentStates = 0x1058;

            WeaponId = 0x1e0;
            WeaponEntityComp = 0x1370;
            CurrWeapon = 0x5d8;
            CurrentWeaponReplicated = 0x5d8;
            ShootWeaponEntityComp = 0x1370;
            WeaponManagerComponent = 0x2608;

            CurBulletNumInClip = 0x8;
            CurUseWeaponLogicSocketRep = 0x320;

            bIsWeaponFiring = 0x1830;
            bIsGunADS = 0x1134;

            ShootMode = 0x10e9;
            BulletFireSpeed = 0x560;
            ShootInterval = 0x5a0;
            BulletTrackDistanceFix = 0x930;

            AccessoriesVRecoilFactor = 0xbc8;
            AccessoriesHRecoilFactor = 0xbd0;
            AccessoriesRecoveryFactor = 0xbcc;

            GameDeviationFactor = 0xc2c;
            RecoilKickADS = 0xcf0;
            ExtraHitPerformScale = 0xcf8;
            AnimationKick = 0xcf4;

            SRecoilInfo = 0xb60;
            POV = 0x10;
            MinimalViewInfo = 0x10;
            CameraCache = 0x520;

            FieldOfView = 0x39c;
            ViewPitchMin = 0x1d4c;
            ViewPitchMax = 0x1d50;
            ViewYawMin = 0x1d54;
            ViewYawMax = 0x1d58;

            LastRenderTime = 0x488;
            CurrentVehicle = 0xeb0;
            VehicleCommon = 0xc08;

            VHealth = 0x354;
            VHealthMax = 0x350;
            VFuel = 0x43c;
            VFuelMax = 0x438;

            GameReplayType = 0xa1c;
            MaxBulletImpactFXClampDistance = 0xce0;
            CustomTimeDilation = 0xe4;

            bIsEngineStarted = 0xbc8;
            ExtraBoostFactor = 0x20a4;

            NearDeathBreath = 0x1c30;
            NearDeathComponent = 0x1c18;
            BreathMax = 0x1cc;

            ReplicatedMovement = 0x110;
            Controller = 0x4e8;
            ControlRotation = 0x4e0;
            AimControlRotationAdditive = 0x211c;

            FixAttachInfoList = 0x200;
            UploadInterval = 0x1e0;
            PickUpDataList = 0x968;
            SwitchWeaponSpeedScale = 0x2c3c;
            ThirdPersonCameraComponent = 0x1d78;

            DrawShootLineTime = 0x13c;
        }
    }
}

#endif
