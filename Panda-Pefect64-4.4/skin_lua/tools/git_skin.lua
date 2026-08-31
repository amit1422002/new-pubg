
_G.skinIdMappings = {
    [101004]={101004,1101004046,1101004226,1101004236,1101004062,1101004078,1101004086,1101004098,1101004138,1101004163,1101004201,1101004209,1101004218},
    [101001]={101001,1101001089,1101001213,1101001172,1101001127,1101001142,1101001153,1101001115,1101001102,1101001230,1101001241},
    [101003]={101003,1103003208,1101003195,1101003187,1101003098,1101003166,1101003069,1101003218,1101003079,1101003118,1101003145,1101003180,1101003056},
    [103001]={103001,1103001191,1103001101,1103001178,1103001145},
    [102002]={102002,1102002136,1102002043,1102002061,1102002424},
    [103002]={103002,1103002030,1103002087,1103002105,1103002112},
    [103003]={103003,1103003042,1103003087,1103003062,1103003022,1103003051,1103003030,1103003079},
    [101008]={101008,1101008079,1101008126,1101008104,1101008146,1101008026,1101008061,1101008116,1101008051},
    [102003]={102003,1102003019,1102003030,1102003064,1102003079},
    [105010]={105010,1105010018,1105010007},
    [102004]={102004,1102004017,1102004033},
    [105002]={105002,1105002090,1105002075,1105002018,1105002034,1105002057,1105002062},
    [105001]={105001,1105001047,1105001068,1105001033,1105001061},
    [101006]={101006,1101006061,1101006074,1101006043,1101006032,1101006084},
    [104004]={104004,1104004034,1104004015,1104004040}
}
_G.skinIdMappings2 = _G.skinIdMappings
_G.WeaponSkinMap = _G.skinIdMappings
_G.WeaponSkinIndex = _G.WeaponSkinIndex or {}
_G.skinIdCache2 = _G.skinIdCache2 or {}
_G.killCountInfo = _G.killCountInfo or {}
_G.lastFileContent = ""
_G.isFileWatcherActive = false
_G.WelcomeShown = false
_G.MatchEndMessageShown = false
_G.DeadBoxSkins = _G.DeadBoxSkins or {}
_G.AlreadyChangedSet = _G.AlreadyChangedSet or {}
_G.LastKillTime = {}
_G.TargetLobbyThemeID = 202408001
_G.LastAppliedThemeID = nil
_G.LastBackApplyValue = 0
_G.CurrentBagApplicationValue = 0
_G.LastHelmetApplyValue = 0
_G.CurrentHelmetApplicationValue = 0
_G.OutfitIndex = _G.OutfitIndex or {Suit=1,Bag=1,Helmet=1,Parachut=1,Pet=1}
_G.LastAppliedPet = 0
_G.skinIdCache = _G.skinIdCache or {}
_G.g_parts = _G.g_parts or {}
_G.lastAppliedAttachments = _G.lastAppliedAttachments or {}
_G.lastAppliedSkin = _G.lastAppliedSkin or {}
_G.CurrentEquipVehicleID = 0

_G.SuitSkin = 0
_G.HatSkin = 0
_G.FaceSkin = 0
_G.MaskSkin = 0
_G.GlovesSkin = 0
_G.PantSkin = 0
_G.ShoeSkin = 0
_G.ParachuteSkin = 0
_G.GliderSkin = 0
_G.BackpackSkin = 0
_G.HelmetSkin = 0
_G.Backpack1Skin = 0
_G.Backpack2Skin = 0
_G.Backpack3Skin = 0
_G.Helmet1Skin = 0
_G.Helmet2Skin = 0
_G.Helmet3Skin = 0
_G.Emote1Skin = 0
_G.Emote2Skin = 0
_G.Emote3Skin = 0
_G.PetSkin = 0
_G.HallEffectSkin = 0

_G.muzzles = {
    id_flash_hider = { 201010, 201005, 201004 },
    id_compensator = { 201009, 201003, 201002 },
    id_suppressor = { 201011, 201006, 201007 }
}
_G.foregrips = {
    id_Angledforegrip = 202001, id_thumb_grip = 202006, id_vertical_grip = 202002,
    id_light_grip = 202004, id_half_grip = 202005, id_ergonomic_grip = 202051, id_laser_sight = 202007
}
_G.magazines = {
    id_expanded_mag = { 204011, 204007, 204004 }, id_quick_mag = { 204012, 204008, 204005 },
    id_expanded_quick_mag = { 204013, 204009, 204006 }
}
_G.scopes = {
    id_reddot = 203001, id_holo = 203002, id_2x = 203003, id_3x = 203014,
    id_4x = 203004, id_6x = 203015, id_8x = 203005
}
_G.stock = {
    id_microStock = 205001, id_tactical = 205002, id_bulletloop = 204014, id_CheekPad = 205003
}

_G.CustSlotType = {
    NONE = 0, HeadEquipemtSlot = 1, HairEquipemtSlot = 2, HatEquipemtSlot = 3,
    FaceEquipemtSlot = 4, ClothesEquipemtSlot = 5, PantsEquipemtSlot = 6,
    ShoesEquipemtSlot = 7, BackpackEquipemtSlot = 8, HelmetEquipemtSlot = 9,
    ArmorEquipemtSlot = 10, ParachuteEquipemtSlot = 11, GlassEquipemtSlot = 12,
    NightVisionEquipemtSlot = 13, BeardEquipemtSlot = 14, GlideEquipemtSlot = 15,
    HandEffectEquipemtSlot = 16, BackPack_PendantSlot = 17, MechaChestSlot = 18,
    MechaArmSlot = 19, MechaLegSlot = 20, MechaInnerSuitSlot = 21,
    FootEffectEquipemtSlot = 22, MaxSlotNum = 23, VehicleCut = 24,
    UnderClothSlot = 25, UnderPantsSlot = 26, SimpleSlotMax = 27, MAX = 28
}

local lastConfig = {}
local ItemUpgradeSystem = nil

-- ============================================================
-- [NEW] SkinAttachmentMap: skinID -> { attachmentName -> attachmentID }
-- Ye attachments.txt se load hoga
-- ============================================================
_G.SkinAttachmentMap = _G.SkinAttachmentMap or {}

local function LoadAttachmentFile(configPath)
    -- config.ini ke saath wali directory mein attachments.txt dhundho
    local dir = configPath:match("^(.*)/[^/]+$") or ""
    local attachPath = dir .. "/attachments.txt"
    
    local file = io.open(attachPath, 'r')
    if not file then return end
    
    local content = file:read('*all')
    file:close()
    
    -- Reset map
    _G.SkinAttachmentMap = {}
    
    local currentSkinID = nil
    for line in content:gmatch('[^\r\n]+') do
        -- Skip comments/separators
        if not line:match('^%s*#') and line:match('%S') then
            -- Check if it's a skin line: just a number (skin ID) optionally with |
            local skinID = line:match('^%s*(%d+)%s*|') or line:match('^%s*(%d+)%s*$')
            if skinID and #skinID >= 9 then
                -- Could be skin ID (9-10 digits) or attachment ID
                -- Skin IDs in attachments.txt are under #skin# section: e.g. 1101001116
                -- We detect by context - after a #---skin---# comment, next line is skin
                -- Actually the format is: skinID | skin name on one line, then attachment lines
                -- Skin line has long name after |, attachment line has short name
                -- Simpler: if line has 2 pipes => attachment line, if 1 pipe => could be skin
                local parts = {}
                for p in line:gmatch('[^|]+') do
                    table.insert(parts, p:match('^%s*(.-)%s*$'))
                end
                if #parts == 2 then
                    -- This is a skin line: "skinID | Skin Name - Gun"
                    currentSkinID = tonumber(parts[1])
                    if currentSkinID then
                        _G.SkinAttachmentMap[currentSkinID] = _G.SkinAttachmentMap[currentSkinID] or {}
                    end
                elseif #parts == 3 and currentSkinID then
                    -- This is an attachment line: "attachID | hexval | Attachment Name"
                    local attachID = tonumber(parts[1])
                    local attachName = parts[3]
                    if attachID and attachName and attachName ~= "" then
                        _G.SkinAttachmentMap[currentSkinID][attachName] = attachID
                    end
                end
            end
        end
    end
end
_G.LoadAttachmentFile = LoadAttachmentFile

-- ============================================================
-- [FIXED] WeaponBaseIDMap - config key -> weapon base ID
-- Ye map ReadConfigFile mein use hoga
-- ============================================================
_G.WeaponBaseIDMap = {
    M416=101004, AKM=101001, SCAR=101003, M16A4=101002,
    GROZA=101005, AUG=101006, QBZ=101007, M762=101008,
    MK47=101009, G36C=101010, FAMAS=101011, ACE32=101102,
    Kar98=103001, M24=103002, AWM=103003, SKS=103004,
    VSS=103005, Mini14=103006, MK14=103007, SLR=103009,
    QBU=103010, MK12=103011, AMR=103012, Mosin=103013,
    UZI=102001, UMP=102002, Vector=102003, Thompson=102004,
    Bizon=102005, MP5K=102007, P90=102009,
    S12K=104003, DBS=104004, S1897=104001, S686=104002,
    M249=105002, DP28=105001, MG3=105010,
    Pan=106001, Machete=106003, Crowbar=106002, Sickle=106004
}

-- ============================================================
-- [FIXED] get_skin_id - ab direct skin ID use hogi
-- User config mein dega: M416=1101004046
-- Yeh function usse directly return karega
-- ============================================================
local function get_skin_id(weaponID)
    if not weaponID then return nil end
    -- WeaponSkinIndex[weaponBaseID] = skinID (as given in config)
    local skinID = (_G.WeaponSkinIndex and _G.WeaponSkinIndex[weaponID]) or weaponID

    -- Agar skinID = weaponID (no skin configured), return as is
    if skinID == weaponID then return skinID end

    -- Download kar skin ko agar cache mein nahi
    if not _G.skinIdCache2[skinID] then
        pcall(_G.download_item, skinID)
        _G.skinIdCache2[skinID] = true
    end
    return skinID
end
_G.get_skin_id = get_skin_id
_G.get_skin_id2 = get_skin_id

local function InitItemUpgradeSystem()
    pcall(function()
        local ModuleManager = require("client.module_framework.ModuleManager")
        if ModuleManager then
            ItemUpgradeSystem = ModuleManager.GetModule(ModuleManager.CommonModuleConfig.ItemUpgradeSystem)
            if ItemUpgradeSystem then
                ItemUpgradeSystem:DefineAndResetData()
                ItemUpgradeSystem:OnInitialize()
            end
        end
    end)
end

local function get_group_id(itemId)
    if not ItemUpgradeSystem or not itemId then return nil end
    pcall(function()
        local itemcfg = ItemUpgradeSystem:GetUpgradeCfg(itemId)
        if itemcfg and itemcfg.GroupID then return itemcfg.GroupID end
    end)
    return nil
end

function _G.InitParts(groupId, itemId)
    if not itemId then return _G.g_parts end
    if _G.g_parts[itemId] and next(_G.g_parts[itemId]) then return _G.g_parts end
    if not _G.g_parts[itemId] then _G.g_parts[itemId] = {} end
    pcall(function()
        local realGroupId = groupId or get_group_id(itemId)
        if ItemUpgradeSystem and ItemUpgradeSystem.IsWeaponIsRefit and ItemUpgradeSystem:IsWeaponIsRefit(itemId) then
            realGroupId = ItemUpgradeSystem:GetNormalGroupID(realGroupId)
        end
        local CDataTable = _G.CDataTable or require("client.slua.config.ClientConfig.data_mgr")
        local cfg = CDataTable.GetTableByFilter("ItemUpgradeUnLockConfig", "GroupID", realGroupId)
        if cfg then
            for _, info in pairs(cfg) do
                local partId = info.PartId
                if ItemUpgradeSystem and ItemUpgradeSystem.IsWeaponIsRefit and ItemUpgradeSystem:IsWeaponIsRefit(itemId) then
                    local switched = ItemUpgradeSystem:PartIDSwitch(partId, true)
                    if switched and switched ~= partId then partId = switched end
                end
                local item = CDataTable.GetTableData("Item", partId)
                if item then _G.g_parts[itemId][item.ItemName] = partId end
            end
        end
    end)
    return _G.g_parts
end

function _G.GetSlotFromSkinID(skinid, stock)
    if not skinid or not stock then return 0 end
    local attachmentTypeMap = {
        [1] = {291004,291102,291001,291006,291005,291002,293003,293004,293009,293007,293005,293006,295001,295002,291007,291003,292002,292003,291011,291008},
        [2] = {205005,205102,205007,205009,205006},
        [3] = {203008,203009,203006,203022,203010}
    }
    local targetAttachmentIDs = attachmentTypeMap[stock]
    if not targetAttachmentIDs then return 0 end
    local UAvatarUtils = import("AvatarUtils")
    if not UAvatarUtils then return 0 end
    local uCurWeaponDefaultAttachmentList = UAvatarUtils.GetWeaponAvatarDefaultAttachmentSkin(skinid, {}, false) or {}
    for _, targetAttachmentID in ipairs(targetAttachmentIDs) do
        for attachmentID, attachmentSkinID in pairs(uCurWeaponDefaultAttachmentList) do
            if attachmentID == targetAttachmentID then return attachmentSkinID end
        end
    end
    return 0
end

-- ============================================================
-- [NEW] GetAttachFromMap - attachments.txt se attachment ID lo
-- avatarid = current skin ID (e.g. 1101001213)
-- attachName = "Flash Hider", "4x Scope", "Extended Mag" etc.
-- Returns: attachmentID (number) or nil
-- ============================================================
local function GetAttachFromMap(avatarid, attachName)
    if not avatarid or not attachName then return nil end
    local skinMap = _G.SkinAttachmentMap and _G.SkinAttachmentMap[avatarid]
    if not skinMap then return nil end
    local id = skinMap[attachName]
    if id and id ~= 0 then
        if not _G.skinIdCache2[id] then
            pcall(_G.download_item, id)
            _G.skinIdCache2[id] = true
        end
        return id
    end
    return nil
end

local function resolveAttachNameFromBaseId(baseId)
    if not baseId then return nil end
    for _, id in ipairs(_G.muzzles.id_flash_hider) do
        if id == baseId then return "Flash Hider" end
    end
    for _, id in ipairs(_G.muzzles.id_compensator) do
        if id == baseId then return "Compensator" end
    end
    for _, id in ipairs(_G.muzzles.id_suppressor) do
        if id == baseId then return "Suppressor" end
    end
    if baseId == _G.foregrips.id_Angledforegrip then return "Angled Foregrip" end
    if baseId == _G.foregrips.id_thumb_grip then return "Thumb Grip" end
    if baseId == _G.foregrips.id_vertical_grip then return "Vertical Foregrip" end
    if baseId == _G.foregrips.id_light_grip then return "Light Grip" end
    if baseId == _G.foregrips.id_half_grip then return "Half Grip" end
    if baseId == _G.foregrips.id_ergonomic_grip then return "Ergonomic Grip" end
    if baseId == _G.foregrips.id_laser_sight then return "Laser Sight" end
    for _, id in ipairs(_G.magazines.id_expanded_mag) do
        if id == baseId then return "Extended Mag" end
    end
    for _, id in ipairs(_G.magazines.id_quick_mag) do
        if id == baseId then return "Quickdraw Mag" end
    end
    for _, id in ipairs(_G.magazines.id_expanded_quick_mag) do
        if id == baseId then return "Extended Quickdraw Mag" end
    end
    if baseId == _G.scopes.id_reddot then return "Red Dot Sight" end
    if baseId == _G.scopes.id_holo then return "Holographic Sight" end
    if baseId == _G.scopes.id_2x then return "2x Scope" end
    if baseId == _G.scopes.id_3x then return "3x Scope" end
    if baseId == _G.scopes.id_4x then return "4x Scope" end
    if baseId == _G.scopes.id_6x then return "6x Scope" end
    if baseId == _G.scopes.id_8x then return "8x Scope" end
    if baseId == _G.stock.id_microStock then return "Stock" end
    if baseId == _G.stock.id_tactical then return "Tactical Stock" end
    if baseId == _G.stock.id_bulletloop then return "Bullet Loop" end
    if baseId == _G.stock.id_CheekPad then return "Cheek Pad" end
    return nil
end

local function normalizeAttachName(name)
    if not name or name == '' then return nil end
    local n = string.lower(name)
    if n:find('flash hider', 1, true) then return 'Flash Hider' end
    if n:find('compensator', 1, true) then return 'Compensator' end
    if n:find('suppressor', 1, true) then return 'Suppressor' end
    if n:find('angled', 1, true) and n:find('grip', 1, true) then return 'Angled Foregrip' end
    if n:find('vertical', 1, true) and n:find('grip', 1, true) then return 'Vertical Foregrip' end
    if n:find('thumb', 1, true) and n:find('grip', 1, true) then return 'Thumb Grip' end
    if n:find('light grip', 1, true) then return 'Light Grip' end
    if n:find('half grip', 1, true) then return 'Half Grip' end
    if n:find('ergonomic', 1, true) then return 'Ergonomic Grip' end
    if n:find('laser', 1, true) then return 'Laser Sight' end
    if n:find('extended quickdraw', 1, true) then return 'Extended Quickdraw Mag' end
    if n:find('extended quick', 1, true) then return 'Extended Quickdraw Mag' end
    if n:find('quickdraw', 1, true) then return 'Quickdraw Mag' end
    if n:find('extended mag', 1, true) or (n:find('extended', 1, true) and n:find('mag', 1, true)) then return 'Extended Mag' end
    if n:find('red dot', 1, true) then return 'Red Dot Sight' end
    if n:find('holographic', 1, true) or n == 'holo' then return 'Holographic Sight' end
    if n:find('8x', 1, true) then return '8x Scope' end
    if n:find('6x', 1, true) then return '6x Scope' end
    if n:find('4x', 1, true) then return '4x Scope' end
    if n:find('3x', 1, true) then return '3x Scope' end
    if n:find('2x', 1, true) then return '2x Scope' end
    if n:find('tactical stock', 1, true) then return 'Tactical Stock' end
    if n:find('bullet loop', 1, true) then return 'Bullet Loop' end
    if n:find('cheek pad', 1, true) then return 'Cheek Pad' end
    if n:find('stock', 1, true) then return 'Stock' end
    return name
end

local function lookupAttachPart(avatarid, ...)
    for i = 1, select('#', ...) do
        local name = select(i, ...)
        local fromMap = GetAttachFromMap(avatarid, name)
        if fromMap then return fromMap end
        local parts = _G.g_parts[avatarid]
        if parts then
            if parts[name] then return parts[name] end
            for k, v in pairs(parts) do
                local canon = normalizeAttachName(k)
                if k == name or canon == name or k:find(name, 1, true) or name:find(k, 1, true) then
                    return v
                end
            end
        end
    end
    return nil
end

local function classifyAttachmentBaseId(itemId)
    if not itemId or itemId == 0 then return nil end
    if itemId >= 10000000 then return nil end
    for _, id in ipairs(_G.muzzles.id_flash_hider) do if itemId == id then return 'muzzle' end end
    for _, id in ipairs(_G.muzzles.id_compensator) do if itemId == id then return 'muzzle' end end
    for _, id in ipairs(_G.muzzles.id_suppressor) do if itemId == id then return 'muzzle' end end
    if itemId == _G.foregrips.id_Angledforegrip or itemId == _G.foregrips.id_thumb_grip
        or itemId == _G.foregrips.id_vertical_grip or itemId == _G.foregrips.id_light_grip
        or itemId == _G.foregrips.id_half_grip or itemId == _G.foregrips.id_ergonomic_grip
        or itemId == _G.foregrips.id_laser_sight then return 'grip' end
    for _, ids in pairs(_G.magazines) do
        for _, id in ipairs(ids) do if itemId == id then return 'mag' end end
    end
    for _, sid in pairs(_G.scopes) do if itemId == sid then return 'scope' end end
    if itemId == _G.stock.id_microStock or itemId == _G.stock.id_tactical
        or itemId == _G.stock.id_bulletloop or itemId == _G.stock.id_CheekPad then return 'stock' end
    return nil
end

-- BGMI synData slots: 0=muzzle 1=scope 2=mag 3=stock 4=grip
local ATTACH_SLOT_KIND = { 'muzzle', 'scope', 'mag', 'stock', 'grip' }

-- base attachment ID -> skin attachment ID (builtin + skin_attachment_maps.lua)
_G.SkinAttachmentBaseOverrides = _G.SkinAttachmentBaseOverrides or {}

-- config.ini skin ID -> attachment map skin ID (when they differ)
local SKIN_ATTACH_ALIASES = {
    [1103003042] = 1103003087, -- AWM
    [1103001191] = 1103001179, -- Kar98
    [1103002030] = 1103002087, -- M24
    [1101008126] = 1101008051, -- M762
}

local function resolveAttachSkinId(skinId)
    if not skinId then return skinId end
    local alias = SKIN_ATTACH_ALIASES[skinId]
    return alias or skinId
end

local function compileLuaChunk(content, path)
    if not content or content == '' then return nil end
    local src = content
    if not src:match('^%s*return') then
        src = 'return ' .. src
    end
    local tag = path and ('@' .. path) or 'chunk'
    if loadstring then
        return loadstring(src, tag)
    end
    if load then
        return load(src, tag)
    end
    return nil
end

local function mergeAttachmentMapFile(data)
    if type(data) ~= 'table' then return 0 end
    local n = 0
    for skinId, map in pairs(data) do
        if type(map) == 'table' then
            local dest = _G.SkinAttachmentBaseOverrides[skinId] or {}
            for baseId, skinAttachId in pairs(map) do
                dest[baseId] = skinAttachId
                n = n + 1
            end
            _G.SkinAttachmentBaseOverrides[skinId] = dest
        end
    end
    return n
end

-- ATTACH_BUILTIN_BEGIN
local function loadBuiltinAttachmentMaps()
    if _G.__builtinAttachLoaded then return end
    mergeAttachmentMapFile({
        [1101001213] = {
            [201009] = 1010012068,
            [201010] = 1010012067,
            [201011] = 1010012069,
            [203001] = 1010012066,
            [203002] = 1010012065,
            [203003] = 1010012064,
            [203004] = 1010012062,
            [203014] = 1010012063,
            [203015] = 1010012075,
            [204011] = 1010012070,
            [204012] = 1010012072,
            [204013] = 1010012073,
        },
        [1101002081] = {
            [201009] = 1010020769,
            [201010] = 1010020768,
            [201011] = 1010020770,
            [203001] = 1010020759,
            [203002] = 1010020758,
            [203003] = 1010020757,
            [203004] = 1010020755,
            [203014] = 1010020756,
            [204011] = 1010020760,
            [204012] = 1010020766,
            [204013] = 1010020767,
            [205002] = 1010020775,
        },
        [1101003195] = {
            [201009] = 1010031911,
            [201010] = 1010031912,
            [201011] = 1010031913,
            [202001] = 1010031914,
            [202002] = 1010031916,
            [202004] = 1010031918,
            [202005] = 1010031919,
            [202006] = 1010031915,
            [203001] = 1010031906,
            [203002] = 1010031905,
            [203003] = 1010031904,
            [203004] = 1010031902,
            [203014] = 1010031903,
            [203015] = 1010031901,
            [204011] = 1010031907,
            [204012] = 1010031908,
            [204013] = 1010031909,
        },
        [1101004046] = {
            [201009] = 1010040475,
            [201010] = 1010040474,
            [201011] = 1010040476,
            [202001] = 1010040477,
            [202002] = 1010040479,
            [202004] = 1010040482,
            [202005] = 1010040483,
            [202006] = 1010040478,
            [203001] = 1010040470,
            [203002] = 1010040469,
            [203003] = 1010040468,
            [203004] = 1010040466,
            [203008] = 1010040462,
            [203014] = 1010040467,
            [203015] = 1010040481,
            [204011] = 1010040471,
            [204012] = 1010040472,
            [204013] = 1010040473,
            [205002] = 1010040480,
            [205005] = 1010040463,
            [291004] = 1010040461,
        },
        [1101005052] = {
            [201011] = 1010050467,
            [203002] = 1010050465,
            [203003] = 1010050464,
            [203004] = 1010050462,
            [203014] = 1010050463,
            [203015] = 1010050473,
            [204011] = 1010050468,
            [204012] = 1010050469,
            [204013] = 1010050470,
        },
        [1101006075] = {
            [201009] = 1010060701,
            [201010] = 1010060702,
            [201011] = 1010060703,
            [202001] = 1010060704,
            [202002] = 1010060706,
            [202004] = 1010060708,
            [202005] = 1010060709,
            [202006] = 1010060705,
            [203001] = 1010060696,
            [203002] = 1010060695,
            [203003] = 1010060694,
            [203004] = 1010060692,
            [203014] = 1010060693,
            [203015] = 1010060691,
            [204011] = 1010060697,
            [204012] = 1010060698,
            [204013] = 1010060699,
        },
        [1101007062] = {
            [201009] = 1010070578,
            [201010] = 1010070579,
            [201011] = 1010070581,
            [202001] = 1010070582,
            [202002] = 1010070584,
            [202004] = 1010070585,
            [202005] = 1010070586,
            [202006] = 1010070583,
            [203001] = 1010070574,
            [203002] = 1010070573,
            [203003] = 1010070572,
            [203004] = 1010070569,
            [203014] = 1010070571,
            [203015] = 1010070568,
            [204011] = 1010070575,
            [204012] = 1010070576,
            [204013] = 1010070577,
        },
        [1101100012] = {
            [201009] = 1011020028,
            [201010] = 1011020027,
            [201011] = 1011020029,
            [203001] = 1011020019,
            [203002] = 1011020018,
            [203003] = 1011020017,
            [203004] = 1011020015,
            [203014] = 1011020016,
            [203015] = 1011000053,
        },
        [1101102007] = {
            [201009] = 1011020028,
            [201010] = 1011020027,
            [201011] = 1011020029,
            [202001] = 1011020034,
            [202002] = 1011020036,
            [202004] = 1011020038,
            [202005] = 1011020039,
            [202006] = 1011020035,
            [203001] = 1011020019,
            [203002] = 1011020018,
            [203003] = 1011020017,
            [203004] = 1011020015,
            [203014] = 1011020016,
            [203015] = 1011020014,
            [204011] = 1011020024,
            [204012] = 1011020025,
            [204013] = 1011020026,
            [205002] = 1011020037,
        },
        [1102001120] = {
            [201002] = 1020011138,
            [201004] = 1020011137,
            [201006] = 1020011139,
            [203001] = 1020011133,
            [203002] = 1020011132,
            [204004] = 1020011134,
            [204005] = 1020011135,
            [204006] = 1020011136,
            [205001] = 1020011142,
        },
        [1102002136] = {
            [201002] = 1020021313,
            [201004] = 1020021314,
            [201006] = 1020021315,
            [202001] = 1020021316,
            [202002] = 1020021318,
            [202004] = 1020021323,
            [202005] = 1020021324,
            [202006] = 1020021317,
            [203001] = 1020021306,
            [203002] = 1020021306,
            [203003] = 1020021305,
            [203004] = 1020021303,
            [203014] = 1020021304,
            [203015] = 1020021302,
            [204004] = 1020021308,
            [204005] = 1020021309,
            [204006] = 1020021312,
        },
        [1102003080] = {
            [201002] = 1020030756,
            [201004] = 1020030755,
            [201006] = 1020030758,
            [202002] = 1020030760,
            [202004] = 1020030759,
            [202005] = 1020030757,
            [203001] = 1020030748,
            [203002] = 1020030747,
            [203003] = 1020030746,
            [203004] = 1020030744,
            [203014] = 1020030745,
            [203015] = 1020030764,
            [204004] = 1020030749,
            [204005] = 1020030750,
            [204006] = 1020030754,
            [205002] = 1020030765,
        },
        [1103001179] = {
            [201003] = 1030011739,
            [201005] = 1030011738,
            [201007] = 1030011741,
            [203001] = 1030011737,
            [203002] = 1030011736,
            [203003] = 1030011735,
            [203004] = 1030011733,
            [203005] = 1030011731,
            [203014] = 1030011734,
            [203015] = 1030011732,
            [205003] = 1030011742,
        },
        [1103002087] = {
            [201003] = 1030020825,
            [201005] = 1030020824,
            [201007] = 1030020826,
            [203001] = 1030020818,
            [203002] = 1030020817,
            [203003] = 1030020816,
            [203004] = 1030020814,
            [203005] = 1030020812,
            [203014] = 1030020815,
            [203015] = 1030020813,
            [205003] = 1030020827,
        },
        [1103003087] = {
            [201003] = 1030030826,
            [201005] = 1030030825,
            [201007] = 1030030827,
            [203001] = 1030030818,
            [203002] = 1030030817,
            [203003] = 1030030816,
            [203004] = 1030030814,
            [203005] = 1030030812,
            [203014] = 1030030815,
            [203015] = 1030030813,
            [204007] = 1030030822,
            [204008] = 1030030823,
            [204009] = 1030030824,
            [205003] = 1030030828,
        },
        [1103004037] = {
            [201003] = 1030040316,
            [201005] = 1030040315,
            [201007] = 1030040317,
            [202001] = 1030040326,
            [202002] = 1030040327,
            [202006] = 1030040328,
            [203004] = 1030040314,
            [203005] = 1030040312,
            [203015] = 1030040313,
            [204007] = 1030040319,
            [204008] = 1030040322,
            [204009] = 1030040318,
            [204011] = 1030040324,
            [204012] = 1030040325,
            [204013] = 1030040323,
            [205003] = 1030040329,
        },
        [1103006030] = {
            [201003] = 1030060246,
            [201005] = 1030060245,
            [201007] = 1030060247,
            [203004] = 1030060244,
            [203005] = 1030060242,
            [203015] = 1030060243,
            [204007] = 1030060249,
            [204009] = 1030060248,
            [204011] = 1030060253,
            [204013] = 1030060252,
        },
        [1103007028] = {
            [201003] = 1030070234,
            [201005] = 1030070233,
            [201007] = 1030070235,
            [201009] = 1030070229,
            [201010] = 1030070228,
            [201011] = 1030070232,
            [203001] = 1030070218,
            [203002] = 1030070217,
            [203003] = 1030070216,
            [203004] = 1030070214,
            [203005] = 1030070212,
            [203014] = 1030070215,
            [203015] = 1030070213,
            [204007] = 1030070225,
            [204008] = 1030070226,
            [204009] = 1030070227,
            [204011] = 1030070222,
            [204012] = 1030070223,
            [204013] = 1030070224,
            [205003] = 1030070236,
        },
        [1103012019] = {
            [203001] = 1030120138,
            [203002] = 1030120137,
            [203003] = 1030120136,
            [203004] = 1030120134,
            [203005] = 1030120132,
            [203014] = 1030120135,
            [203015] = 1030120133,
        },
        [1105010019] = {
            [203001] = 1050100144,
            [203002] = 1050100143,
            [203003] = 1050100142,
            [203004] = 1050100139,
            [203014] = 1050100141,
            [203015] = 1050100138,
        },
    })
    _G.__builtinAttachLoaded = true
    if next(_G.SkinAttachmentBaseOverrides) then
        _G.__attachMapsLoaded = true
    end
end
-- ATTACH_BUILTIN_END

local function loadSkinAttachmentMaps()
    pcall(loadBuiltinAttachmentMaps)
    if _G.__attachMapsLoaded then return end
    local paths = {}
    if _G.__SKIN_CONFIG_BASE and _G.__SKIN_CONFIG_BASE ~= '' then
        table.insert(paths, _G.__SKIN_CONFIG_BASE .. 'skin_attachment_maps.lua')
    end
    for _, base in ipairs(loadSkinDataBases()) do
        table.insert(paths, base .. '/skin_attachment_maps.lua')
    end
    for _, path in ipairs(paths) do
        local data = nil
        local f = io.open(path, 'r')
        if f then
            local content = f:read('*all')
            f:close()
            local chunk = compileLuaChunk(content, path)
            if chunk then
                local ok, result = pcall(chunk)
                if ok then data = result end
            end
        end
        if not data and loadfile then
            local chunk = loadfile(path)
            if chunk then
                local ok, result = pcall(chunk)
                if ok then data = result end
            end
        end
        if data then
            local n = mergeAttachmentMapFile(data)
            if n > 0 then
                _G.__attachMapsLoaded = true
                print('[SkinMod] attachment maps loaded: ' .. path .. ' (' .. n .. ' entries)')
                return
            end
        end
    end
end

local function applyBaseOverridesToAttachmentMap(skinId)
    skinId = resolveAttachSkinId(skinId)
    local overrides = _G.SkinAttachmentBaseOverrides and _G.SkinAttachmentBaseOverrides[skinId]
    if not overrides then return end
    _G.SkinAttachmentMap[skinId] = _G.SkinAttachmentMap[skinId] or {}
    for baseId, skinAttachId in pairs(overrides) do
        local name = resolveAttachNameFromBaseId(baseId)
        if baseId == 203008 then name = '4x Scope' end
        if baseId == 291004 then name = 'Extended Mag' end
        if name then
            _G.SkinAttachmentMap[skinId][name] = skinAttachId
        end
    end
end

local function getWeaponAppliedSkinId(weapon, fallback)
    local synData = weapon and weapon.synData
    if synData then
        local slot7 = synData:Get(7)
        if slot7 then
            local id = slua.IndexReference(slot7, "defineID").TypeSpecificID
            if id and id >= 1100000000 then
                return resolveAttachSkinId(id)
            end
        end
    end
    return resolveAttachSkinId(fallback)
end

local function getSkinAttachOverride(avatarid, current_id)
    if not avatarid or not current_id or current_id == 0 then return nil end
    if current_id >= 10000000 then return nil end
    if not _G.__attachMapsLoaded then pcall(loadSkinAttachmentMaps) end
    avatarid = resolveAttachSkinId(avatarid)
    local map = _G.SkinAttachmentBaseOverrides and _G.SkinAttachmentBaseOverrides[avatarid]
    if not map then return nil end
    local id = map[current_id]
    if id and id ~= 0 then
        if not _G.skinIdCache2[id] then
            pcall(_G.download_item, id)
            _G.skinIdCache2[id] = true
        end
        return id
    end
    return nil
end

local function applyAttachmentSkinForSlot(attachIdx, itemId, avatarid)
    local override = getSkinAttachOverride(avatarid, itemId)
    if override and override ~= itemId then
        return override, true
    end
    local kind = classifyAttachmentBaseId(itemId)
    if not kind and attachIdx >= 0 and attachIdx <= 4 then
        kind = ATTACH_SLOT_KIND[attachIdx + 1]
    end
    if kind == 'muzzle' then return _G.get_muzzleid(itemId, avatarid)
    elseif kind == 'grip' then return _G.get_forgripid(itemId, avatarid)
    elseif kind == 'mag' then return _G.get_magazinesid(itemId, avatarid)
    elseif kind == 'stock' then return _G.get_stockid(itemId, avatarid)
    elseif kind == 'scope' then return _G.get_scopeid(itemId, avatarid)
    end
    return itemId, false
end

local function hasAttachmentOverrideMap(skinId)
    skinId = resolveAttachSkinId(skinId)
    local map = _G.SkinAttachmentBaseOverrides and _G.SkinAttachmentBaseOverrides[skinId]
    return map and next(map) ~= nil
end

local function bootstrapAttachmentSkins()
    if not _G.WeaponSkinIndex then return end
    pcall(loadSkinAttachmentMaps)
    _G.__attachmentBootstrapDone = _G.__attachmentBootstrapDone or {}
    local UAvatarUtils = import("AvatarUtils")
    for _, skinId in pairs(_G.WeaponSkinIndex) do
        if skinId and skinId > 1000000000 and not _G.__attachmentBootstrapDone[skinId] then
            pcall(function() applyBaseOverridesToAttachmentMap(skinId) end)
            if not hasAttachmentOverrideMap(skinId) then
                pcall(function() _G.InitParts(get_group_id(skinId), skinId) end)
            end
            if UAvatarUtils and UAvatarUtils.GetWeaponAvatarDefaultAttachmentSkin and not hasAttachmentOverrideMap(skinId) then
                pcall(function()
                    local list = UAvatarUtils.GetWeaponAvatarDefaultAttachmentSkin(skinId, {}, false) or {}
                    _G.SkinAttachmentMap[skinId] = _G.SkinAttachmentMap[skinId] or {}
                    for baseId, skinAttachId in pairs(list) do
                        local name = resolveAttachNameFromBaseId(baseId)
                        if name and skinAttachId and skinAttachId ~= 0 then
                            _G.SkinAttachmentMap[skinId][name] = skinAttachId
                        end
                    end
                end)
            end
            if _G.g_parts[skinId] then
                _G.SkinAttachmentMap[skinId] = _G.SkinAttachmentMap[skinId] or {}
                for partName, partId in pairs(_G.g_parts[skinId]) do
                    local canon = normalizeAttachName(partName) or resolveAttachNameFromBaseId(partId)
                    if canon and partId and partId ~= 0 then
                        _G.SkinAttachmentMap[skinId][canon] = partId
                    end
                end
            end
            _G.__attachmentBootstrapDone[skinId] = true
        end
    end
end
_G.bootstrapAttachmentSkins = bootstrapAttachmentSkins

function _G.get_muzzleid(current_id, avatarid)
    local initial_id = current_id
    _G.InitParts(get_group_id(avatarid), avatarid)
    local function is_in_muzzles_list(muzzle_type)
        for _, id in ipairs(_G.muzzles[muzzle_type]) do
            if current_id == id then return true end
        end
        return false
    end
    local muzzle_type = nil
    if is_in_muzzles_list("id_flash_hider") then muzzle_type = "Flash Hider"
    elseif is_in_muzzles_list("id_compensator") then muzzle_type = "Compensator"
    elseif is_in_muzzles_list("id_suppressor") then muzzle_type = "Suppressor"
    end
    if muzzle_type then
        local found = lookupAttachPart(avatarid, muzzle_type,
            muzzle_type .. " (AR)", muzzle_type .. " (SMG)", muzzle_type .. " (Snipers)")
        if found then current_id = found end
    end
    if initial_id ~= current_id then return current_id, true end
    return current_id, false
end

function _G.get_forgripid(current_id, avatarid)
    local initial_id = current_id
    _G.InitParts(get_group_id(avatarid), avatarid)
    local grip_name = nil
    if current_id == _G.foregrips.id_Angledforegrip then grip_name = "Angled Foregrip"
    elseif current_id == _G.foregrips.id_thumb_grip then grip_name = "Thumb Grip"
    elseif current_id == _G.foregrips.id_vertical_grip then grip_name = "Vertical Foregrip"
    elseif current_id == _G.foregrips.id_light_grip then grip_name = "Light Grip"
    elseif current_id == _G.foregrips.id_half_grip then grip_name = "Half Grip"
    elseif current_id == _G.foregrips.id_ergonomic_grip then grip_name = "Ergonomic Grip"
    elseif current_id == _G.foregrips.id_laser_sight then grip_name = "Laser Sight"
    end
    if grip_name then
        local found = lookupAttachPart(avatarid, grip_name)
        if found then current_id = found end
    end
    if initial_id ~= current_id then return current_id, true end
    return current_id, false
end

function _G.get_magazinesid(current_id, avatarid)
    local initial_id = current_id
    _G.InitParts(get_group_id(avatarid), avatarid)
    local function is_in_magazine_list(mag_type)
        for _, id in ipairs(_G.magazines[mag_type]) do
            if current_id == id then return true end
        end
        return false
    end
    local magazine_type = nil
    if is_in_magazine_list("id_expanded_mag") then magazine_type = "Extended Mag"
    elseif is_in_magazine_list("id_quick_mag") then magazine_type = "Quickdraw Mag"
    elseif is_in_magazine_list("id_expanded_quick_mag") then magazine_type = "Extended Quickdraw Mag"
    end
    if magazine_type then
        local found = lookupAttachPart(avatarid, magazine_type)
        if found then current_id = found end
    end
    if initial_id ~= current_id then return current_id, true end
    return current_id, false
end

function _G.get_scopeid(current_id, avatarid)
    local initial_id = current_id
    _G.InitParts(get_group_id(avatarid), avatarid)
    local scope_name = nil
    if current_id == _G.scopes.id_reddot then scope_name = "Red Dot Sight"
    elseif current_id == _G.scopes.id_holo then scope_name = "Holographic Sight"
    elseif current_id == _G.scopes.id_2x then scope_name = "2x Scope"
    elseif current_id == _G.scopes.id_3x then scope_name = "3x Scope"
    elseif current_id == _G.scopes.id_4x then scope_name = "4x Scope"
    elseif current_id == _G.scopes.id_6x then scope_name = "6x Scope"
    elseif current_id == _G.scopes.id_8x then scope_name = "8x Scope"
    end
    if scope_name then
        local found = lookupAttachPart(avatarid, scope_name)
        if found then current_id = found end
    end
    if initial_id ~= current_id then return current_id, true end
    return current_id, false
end

function _G.get_stockid(current_id, avatarid)
    local initial_id = current_id
    _G.InitParts(get_group_id(avatarid), avatarid)
    local stock_name = nil
    if current_id == _G.stock.id_microStock then stock_name = "Stock"
    elseif current_id == _G.stock.id_tactical then stock_name = "Tactical Stock"
    elseif current_id == _G.stock.id_bulletloop then stock_name = "Bullet Loop"
    elseif current_id == _G.stock.id_CheekPad then stock_name = "Cheek Pad"
    end
    if stock_name then
        local found = lookupAttachPart(avatarid, stock_name)
        if found then current_id = found end
    end
    if initial_id ~= current_id then return current_id, true end
    return current_id, false
end

local function loadSkinDataBases()
    local bases = {}
    local seen = {}
    local function addBase(b)
        if b and b ~= '' and not seen[b] then
            seen[b] = true
            table.insert(bases, b)
        end
    end
    if _G.__SKIN_CONFIG_BASE and _G.__SKIN_CONFIG_BASE ~= '' then
        addBase(_G.__SKIN_CONFIG_BASE:gsub('/$', ''))
    end
    addBase('/storage/emulated/0/Android/data/com.pubg.imobile/files')
    addBase('/data/user/0/com.pubg.imobile/files')
    for _, pathFile in ipairs({
        '/storage/emulated/0/Android/data/com.pubg.imobile/files/skin_data.paths',
        '/data/user/0/com.pubg.imobile/files/skin_data.paths',
    }) do
        local f = io.open(pathFile, 'r')
        if f then
            for line in f:lines() do
                line = line:match('^%s*(.-)%s*$')
                if line and line ~= '' and not line:match('^#') then
                    addBase(line)
                end
            end
            f:close()
        end
    end
    return bases
end

local function skinProbeLog(...)
    local parts = { ... }
    for i = 1, #parts do parts[i] = tostring(parts[i]) end
    local msg = table.concat(parts, ' ')
    local ts = os.date('%H:%M:%S') or '?'
    local line = string.format('[%s] %s\n', ts, msg)
    local paths = {}
    if _G.__SKIN_PROBE_PATH and _G.__SKIN_PROBE_PATH ~= '' then
        table.insert(paths, _G.__SKIN_PROBE_PATH)
    end
    for _, base in ipairs(loadSkinDataBases()) do
        table.insert(paths, base .. '/skin_probe.log')
    end
    for _, path in ipairs(paths) do
        local f = io.open(path, 'a')
        if f then
            f:write(line)
            f:close()
            break
        end
    end
    pcall(function()
        if log and bWriteLog then log(bWriteLog, '[SkinMod] ' .. msg) end
    end)
    print('[SkinMod] ' .. msg)
end
_G.skinProbeLog = skinProbeLog

local function ReadConfigFile(force)
    local now = os.clock()
    if not force and _G.__lastConfigReadClock and (now - _G.__lastConfigReadClock) < 90 then
        return
    end

    local possiblePaths = {}
    for _, base in ipairs(loadSkinDataBases()) do
        table.insert(possiblePaths, base .. '/config.ini')
    end
    
    if _G.__SKIN_CONFIG_BASE and _G.__SKIN_CONFIG_BASE ~= '' then
        table.insert(possiblePaths, 1, _G.__SKIN_CONFIG_BASE .. 'config.ini')
    end

    local configPath = nil
    for _, path in ipairs(possiblePaths) do
        local file = io.open(path, 'r')
        if file then 
            file:close()
            configPath = path
            break 
        end
    end
    
    if not configPath then
        return
    end

    local file = io.open(configPath, 'r')
    if not file then return end
    local content = file:read('*all')
    file:close()

    if not force and content == _G.__lastConfigContent then
        _G.__lastConfigReadClock = now
        return
    end
    _G.__lastConfigContent = content
    _G.__lastConfigReadClock = now
    _G.__attachmentBootstrapDone = {}
    
    local newConfig = {}
    for line in content:gmatch('[^\r\n]+') do
        local key, value = line:match('^%s*([%w_]+)%s*=%s*(%d+)%s*')
        if key and value then newConfig[key] = tonumber(value) end
    end
    
    local oldTheme = _G.TargetLobbyThemeID
    _G.TargetLobbyThemeID = newConfig['LobbyTheme'] or _G.TargetLobbyThemeID or 0
    if oldTheme ~= _G.TargetLobbyThemeID then _G.LastAppliedThemeID = nil end
    local configShirt = newConfig['SHIRT'] or 0
    local lobbyOutfit = tonumber(_G.AddOutfitLastLobbyOutfitRes) or tonumber(_G.__ACTIVE_OUTFIT_RES) or 0
    if lobbyOutfit > 0 then
        _G.SuitSkin = lobbyOutfit
        _G.__ACTIVE_OUTFIT_RES = lobbyOutfit
    elseif configShirt > 0 then
        _G.SuitSkin = configShirt
        _G.__ACTIVE_OUTFIT_RES = configShirt
        _G.AddOutfitLastLobbyOutfitRes = configShirt
    elseif _G.__ACTIVE_OUTFIT_RES and tonumber(_G.__ACTIVE_OUTFIT_RES) > 0 then
        _G.SuitSkin = tonumber(_G.__ACTIVE_OUTFIT_RES)
    else
        _G.SuitSkin = configShirt
    end
    _G.HatSkin      = newConfig['HAT']       or 0
    _G.FaceSkin     = newConfig['FACE']      or 0
    _G.MaskSkin     = newConfig['MASK']      or 0
    _G.GlovesSkin   = newConfig['GLOVES']    or 0
    _G.PantSkin     = newConfig['PANT']      or 0
    _G.ShoeSkin     = newConfig['SHOE']      or 0
    _G.ParachuteSkin= newConfig['PARACHUTE'] or 0
    _G.GliderSkin   = newConfig['GLIDER']    or 0
    _G.Backpack1Skin= newConfig['BACKPACK1'] or 0
    _G.Backpack2Skin= newConfig['BACKPACK2'] or 0
    _G.Backpack3Skin= newConfig['BACKPACK3'] or 0
    _G.Helmet1Skin  = newConfig['HELMET1']   or 0
    _G.Helmet2Skin  = newConfig['HELMET2']   or 0
    _G.Helmet3Skin  = newConfig['HELMET3']   or 0
    _G.Emote1Skin   = newConfig['EMOTE1']    or 0
    _G.Emote2Skin   = newConfig['EMOTE2']    or 0
    _G.Emote3Skin   = newConfig['EMOTE3']    or 0
    local oldPet = _G.PetSkin
    local petVal = newConfig['PET_SKIN'] or newConfig['PET']
    if petVal then
        _G.PetSkin = petVal
        if oldPet ~= _G.PetSkin then _G.LastAppliedPet = 0 end
    end
    _G.HallEffectSkin= newConfig['HALL_EFFECT'] or 0

    -- ============================================================
    -- [FIXED] Gun Skin Logic
    -- Config mein user dega: M416=1101004046 (direct skin ID)
    -- Hum WeaponSkinIndex[weaponBaseID] = skinID store karte hain
    -- Phir game mein jab weapon equip hota hai, skin apply hoti hai
    -- ============================================================
    if not _G.WeaponSkinIndex then _G.WeaponSkinIndex = {} end
    for configKey, weaponBaseID in pairs(_G.WeaponBaseIDMap or {}) do
        local configVal = newConfig[configKey]
        if configVal and configVal ~= 0 then
            _G.WeaponSkinIndex[weaponBaseID] = configVal
            lastConfig[configKey] = configVal
        end
    end

    for k, v in pairs(newConfig) do
        lastConfig[k] = v
    end

    pcall(LoadAttachmentFile, configPath)
    if _G.__SKIN_CONFIG_BASE and _G.__SKIN_CONFIG_BASE ~= '' then
        pcall(LoadAttachmentFile, _G.__SKIN_CONFIG_BASE .. 'config.ini')
    end
    pcall(loadSkinAttachmentMaps)
    pcall(bootstrapAttachmentSkins)
end
_G.ReadConfigFile = ReadConfigFile

local function checkSkinReloadStamp()
    for _, base in ipairs(loadSkinDataBases()) do
        local path = base .. '/skin_reload.stamp'
        local f = io.open(path, 'r')
        if f then
            local stamp = (f:read('*a') or ''):gsub('%s+', '')
            f:close()
            if stamp ~= '' and stamp ~= (_G.__skinReloadStamp or '') then
                _G.__skinReloadStamp = stamp
                ReadConfigFile(true)
                _G.__attachmentBootstrapDone = {}
                return true
            end
        end
    end
    if _G.__SKIN_CONFIG_BASE and _G.__SKIN_CONFIG_BASE ~= '' then
        local f = io.open(_G.__SKIN_CONFIG_BASE .. 'skin_reload.stamp', 'r')
        if f then
            local stamp = (f:read('*a') or ''):gsub('%s+', '')
            f:close()
            if stamp ~= '' and stamp ~= (_G.__skinReloadStamp or '') then
                _G.__skinReloadStamp = stamp
                ReadConfigFile(true)
                _G.__attachmentBootstrapDone = {}
                return true
            end
        end
    end
    return false
end

local function resolveBaseWeaponId(weaponid)
    if not weaponid or weaponid == 0 then return nil end
    if _G.WeaponSkinIndex and _G.WeaponSkinIndex[weaponid] then
        return weaponid
    end
    if _G.skinIdMappings then
        for baseId, skins in pairs(_G.skinIdMappings) do
            if baseId == weaponid then return baseId end
            for i = 1, #skins do
                if skins[i] == weaponid then return baseId end
            end
        end
    end
    if _G.WeaponBaseIDMap then
        for _, baseId in pairs(_G.WeaponBaseIDMap) do
            if baseId == weaponid then return baseId end
        end
    end
    return weaponid
end
_G.resolveBaseWeaponId = resolveBaseWeaponId

-- ============================================================
-- [FIXED] apply_weapon_skin - Yeh function weapon pe skin lagata hai
-- skinId = jo config se aaya (e.g. 1101004046)
-- weaponBaseID = weapon ka base ID (e.g. 101004 for M416)
-- ============================================================
local function apply_weapon_skin(CurWeapon, weaponBaseID)
    if not CurWeapon or not slua.isValid(CurWeapon) then return end
    if not weaponBaseID then return end

    weaponBaseID = resolveBaseWeaponId(weaponBaseID)
    local targetSkinID = _G.WeaponSkinIndex and _G.WeaponSkinIndex[weaponBaseID]
    if not targetSkinID or targetSkinID == 0 then return end

    -- Download skin agar cached nahi
    if not _G.skinIdCache[targetSkinID] then
        pcall(_G.download_item, targetSkinID)
        _G.skinIdCache[targetSkinID] = true
    end

    -- synData:Get(7) mein skin info hoti hai
    local synData = CurWeapon.synData
    if not synData then return end

    local slotData = synData:Get(7)
    if not slotData then return end

    local currentSkinID = slua.IndexReference(slotData, "defineID").TypeSpecificID
    local skinChanged = (currentSkinID ~= targetSkinID)
    if skinChanged then
        slotData.defineID.TypeSpecificID = targetSkinID
        synData:Set(7, slotData)
        pcall(function() CurWeapon:DelayHandleAvatarMeshChanged() end)
        pcall(function()
            local avatar = CurWeapon.WeaponAvatarComponent_BP or CurWeapon.WeaponAvatarComp or CurWeapon.AvatarComponent
            if avatar and avatar.ChangeItemAvatar then
                avatar:ChangeItemAvatar(targetSkinID, true)
            end
        end)
    end
    pcall(_G.apply_attachment, CurWeapon, targetSkinID)
end
_G.apply_weapon_skin = apply_weapon_skin

local function getSkinPlayerController()
    if slua_GameFrontendHUD and slua_GameFrontendHUD.GetPlayerController then
        local pc = slua_GameFrontendHUD:GetPlayerController()
        if pc and slua.isValid(pc) then return pc end
    end
    local ok, GD = pcall(require, "GameLua.GameCore.Data.GameplayData")
    if ok and GD and GD.GetPlayerController then
        local pc = GD.GetPlayerController()
        if pc and slua.isValid(pc) then return pc end
    end
    return nil
end

local function getCharacterFromController(pc)
    if not pc or not slua.isValid(pc) then return nil end
    local ch = nil
    local tryFns = {
        function() return pc.GetPlayerCharacterSafety and pc:GetPlayerCharacterSafety() end,
        function() return pc.GetPlayerCharacter and pc:GetPlayerCharacter() end,
        function() return pc.GetPawn and pc:GetPawn() end,
        function() return pc.K2_GetPawn and pc:K2_GetPawn() end,
        function() return pc.AcknowledgedPawn end,
        function() return pc.Pawn end,
        function()
            if pc.GetViewTarget then
                return pc:GetViewTarget()
            end
        end,
    }
    for _, fn in ipairs(tryFns) do
        pcall(function()
            local c = fn()
            if c and slua.isValid(c) then ch = c end
        end)
        if ch then return ch end
    end
    return nil
end

local function getSkinPlayerCharacter()
    local pc = getSkinPlayerController()
    local ch = getCharacterFromController(pc)
    if ch then return ch end
    pcall(function()
        local GD = require('GameLua.GameCore.Data.GameplayData')
        if not GD then return end
        if GD.GetPlayerCharacter then
            local c = GD.GetPlayerCharacter()
            if c and slua.isValid(c) then ch = c end
        end
        if not ch and GD.GetLocalFocusCharacter then
            local c = GD.GetLocalFocusCharacter()
            if c and slua.isValid(c) then ch = c end
        end
    end)
    return ch
end

local function getMatchPlayerCharacter()
    local ch = nil
    pcall(function()
        if slua_GameFrontendHUD and slua_GameFrontendHUD.GetPlayerController then
            local pc = slua_GameFrontendHUD:GetPlayerController()
            if pc and slua.isValid(pc) then
                ch = getCharacterFromController(pc)
            end
        end
    end)
    if ch and slua.isValid(ch) then return ch end
    ch = getSkinPlayerCharacter()
    if ch and slua.isValid(ch) then return ch end
    pcall(function()
        local GD = require('GameLua.GameCore.Data.GameplayData')
        if GD and GD.GetPlayerCharacter then
            local c = GD.GetPlayerCharacter()
            if c and slua.isValid(c) then ch = c end
        end
    end)
    return (ch and slua.isValid(ch)) and ch or nil
end
_G.getMatchPlayerCharacter = getMatchPlayerCharacter

local function probeActors(reason)
    local now = os.clock()
    if now - (_G.__skinProbeLast or 0) < 5 then return end
    _G.__skinProbeLast = now

    skinProbeLog('=== ACTOR PROBE:', reason or '?')
    skinProbeLog('  HUD:', slua_GameFrontendHUD ~= nil)

    local pcHud, pcHudValid = nil, false
    pcall(function()
        if slua_GameFrontendHUD and slua_GameFrontendHUD.GetPlayerController then
            pcHud = slua_GameFrontendHUD:GetPlayerController()
            pcHudValid = pcHud ~= nil and slua.isValid(pcHud)
        end
    end)
    skinProbeLog('  PC@HUD:', pcHud ~= nil, 'valid=' .. tostring(pcHudValid))

    local pcGD = nil
    pcall(function()
        local GD = require('GameLua.GameCore.Data.GameplayData')
        if GD and GD.GetPlayerController then
            pcGD = GD.GetPlayerController()
        end
    end)
    skinProbeLog('  PC@GameplayData:', pcGD ~= nil)

    local pc = getSkinPlayerController()
    local ch = getSkinPlayerCharacter()
    local chValid = ch ~= nil and slua.isValid(ch)
    skinProbeLog('  Character:', ch ~= nil, 'valid=' .. tostring(chValid))
    local pawn = getCharacterFromController(pc)
    skinProbeLog('  Pawn:', pawn ~= nil, pawn and slua.isValid(pawn))
    local targets = collectOutfitTargets and collectOutfitTargets() or {}
    skinProbeLog('  OutfitTargets:', #targets)

    if chValid then
        local w = nil
        pcall(function() w = ch:GetCurrentWeapon() end)
        if w and slua.isValid(w) then
            local defineId, slotSkin = 0, 0
            pcall(function()
                defineId = w:GetItemDefineID().TypeSpecificID
                slotSkin = slua.IndexReference(w.synData:Get(7), 'defineID').TypeSpecificID
            end)
            local base = resolveBaseWeaponId(defineId)
            local target = _G.WeaponSkinIndex and _G.WeaponSkinIndex[base]
            skinProbeLog('  Weapon: defineId=' .. tostring(defineId),
                'slotSkin=' .. tostring(slotSkin), 'base=' .. tostring(base),
                'target=' .. tostring(target))
        else
            skinProbeLog('  Weapon: none')
        end
        pcall(function()
            for slotIdx = 0, 1 do
                local w2 = ch:GetWeaponByIndex(slotIdx)
                if w2 and slua.isValid(w2) then
                    local id = 0
                    pcall(function() id = w2:GetItemDefineID().TypeSpecificID end)
                    skinProbeLog('  Slot' .. slotIdx .. ':', id)
                end
            end
        end)
    end

    local skinCount = 0
    if _G.WeaponSkinIndex then
        for _ in pairs(_G.WeaponSkinIndex) do skinCount = skinCount + 1 end
    end
    skinProbeLog('  WeaponSkinIndex:', skinCount,
        'timers=' .. tostring(_G.__SKIN_TIMERS_STARTED),
        'boot=' .. tostring(_G.__SKIN_BOOTSTRAPPED))
end
_G.probeActors = probeActors

function table.contains(table, element)
    for _, value in ipairs(table) do
        if value == element then return true end
    end
    return false
end

local function locationsClose(loc1, loc2, tolerance)
    local dx = loc1.X - loc2.X
    local dy = loc1.Y - loc2.Y
    local dz = loc1.Z - loc2.Z
    return dx * dx + dy * dy + dz * dz < tolerance * tolerance
end

local GEAR_BASE_BAG = { [1]=501001, [2]=501002, [3]=501003 }
local GEAR_BASE_HELM = { [1]=502001, [2]=502002, [3]=502003 }

local function isConfiguredGearSkin(itemId)
    itemId = tonumber(itemId) or 0
    if itemId <= 0 then return false end
    local skins = {
        _G.Backpack1Skin, _G.Backpack2Skin, _G.Backpack3Skin,
        _G.Helmet1Skin, _G.Helmet2Skin, _G.Helmet3Skin,
    }
    for _, s in ipairs(skins) do
        if s and tonumber(s) == itemId then return true end
    end
    return false
end

local function resolveGearLevelFromBase(addId, baseMap, getLevelFn, kind)
    addId = tonumber(addId) or 0
    if addId <= 0 then return nil end
    if addId >= 1501000000 then return nil end
    for lvl, base in pairs(baseMap) do
        if addId == base then return lvl end
    end
    if kind == "bag" then
        if addId < 501001 or addId > 501999 then return nil end
    elseif kind == "helmet" then
        if addId < 502001 or addId > 502999 then return nil end
    end
    if getLevelFn then
        local lvl = getLevelFn(addId)
        if lvl and lvl > 0 and lvl <= 3 then return lvl end
    end
    return nil
end

local function resolveGearLevelFromSkinRes(itemId, kind)
    itemId = tonumber(itemId) or 0
    if itemId <= 0 then return nil end
    local s = tostring(itemId)
    if kind == "bag" then
        if s:sub(1, 7) == "1501001" then return 1 end
        if s:sub(1, 7) == "1501002" then return 2 end
        if s:sub(1, 7) == "1501003" then return 3 end
    elseif kind == "helmet" then
        if s:sub(1, 7) == "1502001" then return 1 end
        if s:sub(1, 7) == "1502002" then return 2 end
        if s:sub(1, 7) == "1502003" then return 3 end
    end
    return nil
end

local function resolveConfiguredGearPreviewLevel(kind)
    if kind == "bag" then
        if _G.Backpack3Skin and _G.Backpack3Skin > 0 then return 3, GEAR_BASE_BAG[3] end
        if _G.Backpack2Skin and _G.Backpack2Skin > 0 then return 2, GEAR_BASE_BAG[2] end
        if _G.Backpack1Skin and _G.Backpack1Skin > 0 then return 1, GEAR_BASE_BAG[1] end
    else
        if _G.Helmet3Skin and _G.Helmet3Skin > 0 then return 3, GEAR_BASE_HELM[3] end
        if _G.Helmet2Skin and _G.Helmet2Skin > 0 then return 2, GEAR_BASE_HELM[2] end
        if _G.Helmet1Skin and _G.Helmet1Skin > 0 then return 1, GEAR_BASE_HELM[1] end
    end
    return nil, nil
end

local function playerHasGearItem(uCharacter, itemId, BackpackUtils)
    itemId = tonumber(itemId) or 0
    if itemId <= 0 then return false end
    local found = false
    pcall(function()
        local pc = nil
        if uCharacter and uCharacter.GetPlayerController then pc = uCharacter:GetPlayerController() end
        if (not pc or not slua.isValid(pc)) and slua_GameFrontendHUD then
            pc = slua_GameFrontendHUD:GetPlayerController()
        end
        if not pc or not slua.isValid(pc) then return end
        if BackpackUtils then
            if BackpackUtils.IsItemInBackpack and BackpackUtils.IsItemInBackpack(pc, itemId) then
                found = true return
            end
            if BackpackUtils.HasItemByID and BackpackUtils.HasItemByID(pc, itemId) then
                found = true return
            end
            if BackpackUtils.GetItemCountByItemID and (BackpackUtils.GetItemCountByItemID(pc, itemId) or 0) > 0 then
                found = true return
            end
        end
        local bc = pc.BackpackComponent
        if bc and bc.GetItemCountByItemID and (bc:GetItemCountByItemID(itemId) or 0) > 0 then
            found = true
        end
    end)
    return found
end

local function getGearLevelFromInventory(uCharacter, kind, BackpackUtils)
    local baseMap = kind == "bag" and GEAR_BASE_BAG or GEAR_BASE_HELM
    for lvl = 3, 1, -1 do
        local base = baseMap[lvl]
        if base and playerHasGearItem(uCharacter, base, BackpackUtils) then
            return lvl, base
        end
    end
    return nil, nil
end

local function getEquippedGearLevel(ApplyData, slotId, baseMap, getLevelFn, kind)
    if not ApplyData or not slua.isValid(ApplyData) then return nil, nil end
    local idx = findAvatarSlotIndex(ApplyData, slotId)
    if not idx then return nil, nil end
    local eq = ApplyData:Get(idx)
    if not eq then return nil, nil end
    local addId = tonumber(eq.AdditionalItemID) or 0
    local itemId = tonumber(eq.ItemId) or 0
    if addId > 0 then
        local lvl = resolveGearLevelFromBase(addId, baseMap, getLevelFn, kind)
        if lvl and lvl > 0 then return lvl, baseMap[lvl] or baseMap[1] end
        if getLevelFn then
            local lvl2 = getLevelFn(addId)
            if lvl2 and lvl2 > 0 and lvl2 <= 3 then return lvl2, baseMap[lvl2] or baseMap[1] end
        end
    end
    if itemId > 0 then
        local lvl = resolveGearLevelFromSkinRes(itemId, kind)
        if lvl and lvl > 0 then return lvl, baseMap[lvl] or baseMap[1] end
    end
    return nil, nil
end

local function resolveGearVisualState(uCharacter, ApplyData, BackpackUtils, slots, inMatch)
    local getBagLevel = BackpackUtils and BackpackUtils.GetEquipmentBagLevel or nil
    local getHelmLevel = BackpackUtils and BackpackUtils.GetEquipmentHelmetLevel or nil
    local bagLvl, bagBase, helmLvl, helmBase

    if inMatch then
        bagLvl, bagBase = getCharacterBattleGearLevel(uCharacter, "bag", BackpackUtils)
        if not bagLvl then
            bagLvl, bagBase = getGearLevelFromInventory(uCharacter, "bag", BackpackUtils)
        end
        helmLvl, helmBase = getCharacterBattleGearLevel(uCharacter, "helmet", BackpackUtils)
        if not helmLvl then
            helmLvl, helmBase = getGearLevelFromInventory(uCharacter, "helmet", BackpackUtils)
        end
    else
        bagLvl, bagBase = getEquippedGearLevel(
            ApplyData, slots.BackpackEquipemtSlot, GEAR_BASE_BAG, getBagLevel, "bag")
        helmLvl, helmBase = getEquippedGearLevel(
            ApplyData, slots.HelmetEquipemtSlot, GEAR_BASE_HELM, getHelmLevel, "helmet")
    end

    if ApplyData and slua.isValid(ApplyData) then
        if (not bagLvl or bagLvl <= 0) and getBagLevel then
            local idx = findAvatarSlotIndex(ApplyData, slots.BackpackEquipemtSlot)
            if idx then
                local eq = ApplyData:Get(idx)
                local addId = eq and tonumber(eq.AdditionalItemID) or 0
                if addId > 0 then
                    bagLvl = getBagLevel(addId) or resolveGearLevelFromBase(addId, GEAR_BASE_BAG, getBagLevel, "bag")
                    if bagLvl and bagLvl > 0 then bagBase = GEAR_BASE_BAG[bagLvl] end
                end
            end
        end
        if (not helmLvl or helmLvl <= 0) and getHelmLevel then
            local idx = findAvatarSlotIndex(ApplyData, slots.HelmetEquipemtSlot)
            if idx then
                local eq = ApplyData:Get(idx)
                local addId = eq and tonumber(eq.AdditionalItemID) or 0
                if addId > 0 then
                    helmLvl = getHelmLevel(addId) or resolveGearLevelFromBase(addId, GEAR_BASE_HELM, getHelmLevel, "helmet")
                    if helmLvl and helmLvl > 0 then helmBase = GEAR_BASE_HELM[helmLvl] end
                end
            end
        end
    end

    if not inMatch then
        if not bagLvl then bagLvl, bagBase = resolveConfiguredGearPreviewLevel("bag") end
        if not helmLvl then helmLvl, helmBase = resolveConfiguredGearPreviewLevel("helmet") end
    end

    local bagSkin = pickGearSkinForLevel(bagLvl, _G.Backpack1Skin, _G.Backpack2Skin, _G.Backpack3Skin)
    local helmSkin = pickGearSkinForLevel(helmLvl, _G.Helmet1Skin, _G.Helmet2Skin, _G.Helmet3Skin)

    if not inMatch then
        if (not bagSkin or bagSkin == 0) and (_G.Backpack3Skin or _G.Backpack2Skin or _G.Backpack1Skin) then
            bagSkin = _G.Backpack3Skin or _G.Backpack2Skin or _G.Backpack1Skin
            bagLvl = bagLvl or 3
            bagBase = bagBase or GEAR_BASE_BAG[bagLvl] or GEAR_BASE_BAG[3]
        end
        if (not helmSkin or helmSkin == 0) and (_G.Helmet3Skin or _G.Helmet2Skin or _G.Helmet1Skin) then
            helmSkin = _G.Helmet3Skin or _G.Helmet2Skin or _G.Helmet1Skin
            helmLvl = helmLvl or 3
            helmBase = helmBase or GEAR_BASE_HELM[helmLvl] or GEAR_BASE_HELM[3]
        end
    end

    return bagSkin, bagBase, bagLvl, helmSkin, helmBase, helmLvl
end

local function getCharacterBattleGearLevel(uCharacter, kind, BackpackUtils)
    if not uCharacter or not slua.isValid(uCharacter) then return nil, nil end
    local baseMap = kind == "bag" and GEAR_BASE_BAG or GEAR_BASE_HELM
    local getLevelFn = kind == "bag"
        and (BackpackUtils and BackpackUtils.GetEquipmentBagLevel)
        or (BackpackUtils and BackpackUtils.GetEquipmentHelmetLevel)
    local lvl = nil

    pcall(function()
        if kind == "helmet" then
            if uCharacter.GetHelmetLevel then lvl = uCharacter:GetHelmetLevel() end
            if (not lvl or lvl <= 0) and uCharacter.CurrentHelmetLevel then
                lvl = tonumber(uCharacter.CurrentHelmetLevel)
            end
            if (not lvl or lvl <= 0) and uCharacter.GetEquipHelmetLevel then
                lvl = uCharacter:GetEquipHelmetLevel()
            end
        else
            if uCharacter.GetBagLevel then lvl = uCharacter:GetBagLevel() end
            if (not lvl or lvl <= 0) and uCharacter.CurrentBagLevel then
                lvl = tonumber(uCharacter.CurrentBagLevel)
            end
            if (not lvl or lvl <= 0) and uCharacter.GetEquipBagLevel then
                lvl = uCharacter:GetEquipBagLevel()
            end
        end
        if (not lvl or lvl <= 0) and BackpackUtils then
            if kind == "bag" then
                if BackpackUtils.GetBagLevelByCharacter then lvl = BackpackUtils.GetBagLevelByCharacter(uCharacter) end
                if (not lvl or lvl <= 0) and BackpackUtils.GetCurrentBagLevel then
                    lvl = BackpackUtils.GetCurrentBagLevel(uCharacter)
                end
            else
                if BackpackUtils.GetHelmetLevelByCharacter then lvl = BackpackUtils.GetHelmetLevelByCharacter(uCharacter) end
                if (not lvl or lvl <= 0) and BackpackUtils.GetCurrentHelmetLevel then
                    lvl = BackpackUtils.GetCurrentHelmetLevel(uCharacter)
                end
            end
        end
        if (not lvl or lvl <= 0) and uCharacter.PlayerState then
            local ps = uCharacter.PlayerState
            if kind == "bag" and ps.BagLevel then lvl = tonumber(ps.BagLevel)
            elseif kind == "helmet" and ps.HelmetLevel then lvl = tonumber(ps.HelmetLevel) end
        end
    end)
    if lvl and lvl > 0 and lvl <= 3 then
        return lvl, baseMap[lvl] or baseMap[1]
    end

    local slotLvl, slotBase = nil, nil
    pcall(function()
        local comp = getCharacterAvatarComponent(uCharacter)
        local netData = comp and comp.NetAvatarData
        local applyData = netData and slua.isValid(netData) and netData.SlotSyncData
        if not applyData or not slua.isValid(applyData) then return end
        local slotId = kind == "bag"
            and _G.CustSlotType.BackpackEquipemtSlot
            or _G.CustSlotType.HelmetEquipemtSlot
        slotLvl, slotBase = getEquippedGearLevel(applyData, slotId, baseMap, getLevelFn, kind)
    end)
    if slotLvl and slotLvl > 0 then
        return slotLvl, slotBase
    end
    return nil, nil
end

local function stripModGearSlot(ApplyData, slotId, kind)
    if not ApplyData or not slua.isValid(ApplyData) then return false end
    local idx = findAvatarSlotIndex(ApplyData, slotId)
    if not idx then return false end
    local eq = ApplyData:Get(idx)
    if not eq then return false end
    local addId = tonumber(eq.AdditionalItemID) or 0
    local itemId = tonumber(eq.ItemId) or 0
    if addId > 0 then
        local baseMap = kind == "bag" and GEAR_BASE_BAG or GEAR_BASE_HELM
        for _, base in pairs(baseMap) do
            if addId == base then return false end
        end
        if kind == "bag" and addId >= 501001 and addId <= 501999 then return false end
        if kind == "helmet" and addId >= 502001 and addId <= 502999 then return false end
    end
    if itemId <= 0 then return false end
    if not isConfiguredGearSkin(itemId) and itemId < 1501000000 then return false end
    eq.ItemId = 0
    eq.AdditionalItemID = 0
    ApplyData:Set(idx, eq)
    return true
end

local function unequipModGearVisual(avatarComp, ApplyData, slotId, kind)
    -- Only clear mod cosmetic data on bag/helmet slots.
    -- Do NOT call PutOffEquipmentBySlot — it can strip the whole avatar including outfit.
    return stripModGearSlot(ApplyData, slotId, kind)
end

local function pickGearSkinForLevel(lvl, skin1, skin2, skin3)
    if not lvl or lvl <= 0 then return nil end
    local skin = (lvl == 2 and skin2) or (lvl == 3 and skin3) or skin1
    if skin and skin > 0 then return skin end
    return skin3 or skin2 or skin1
end

local function isInActiveMatch()
    if isInLobby() then return false end
    if _G.isInPlayableGame then
        return _G.isInPlayableGame()
    end
    local okFight, fighting = pcall(function()
        return GameStatus and GameStatus.IsInFightingStatus and GameStatus.IsInFightingStatus()
    end)
    if okFight and fighting then return true end
    if _G.__PLAYABLE_GAME_ACTIVE then return true end
    local ok, gs = pcall(function()
        return slua_GameFrontendHUD and slua_GameFrontendHUD:GetGameState()
    end)
    if not ok or not gs then return false end
    local state = gs.GameModeState
    if not state and gs.GetGameModeState then
        state = gs:GetGameModeState()
    end
    if not state then return false end
    return state ~= 'Lobby' and state ~= 'None'
end

local function isInLobby()
    local okLobby, inLobby = pcall(function()
        return GameStatus and GameStatus.IsInLobbyOrMainCity and GameStatus.IsInLobbyOrMainCity()
    end)
    if okLobby and inLobby then return true end
    local ok, gs = pcall(function()
        return slua_GameFrontendHUD and slua_GameFrontendHUD:GetGameState()
    end)
    if ok and gs then
        local state = gs.GameModeState
        if not state and gs.GetGameModeState then state = gs:GetGameModeState() end
        if state == 'Lobby' or state == 'None' or state == '' then return true end
    end
    return false
end
_G.isInLobby = isInLobby

local function isLobbyCharacter(ch)
    if not ch or not slua.isValid(ch) then return false end
    local isLobby = false
    pcall(function()
        if ch.IsLobbyActor and ch:IsLobbyActor() then isLobby = true end
    end)
    if isLobby then return true end
    pcall(function()
        local cls = ch.GetClass and ch:GetClass()
        if cls then
            local name = tostring(cls:GetName() or '')
            if name:find('Lobby', 1, true) or name:find('MainCity', 1, true) then
                isLobby = true
            end
        end
    end)
    return isLobby
end

local function isInPlayableGame()
    if isInLobby() then
        _G.__PLAYABLE_GAME_ACTIVE = false
        return false
    end
    local ch = getMatchPlayerCharacter()
    if ch and isLobbyCharacter(ch) then
        _G.__PLAYABLE_GAME_ACTIVE = false
        return false
    end
    if _G.__PLAYABLE_GAME_ACTIVE then return true end
    local okFight, fighting = pcall(function()
        return GameStatus and GameStatus.IsInFightingStatus and GameStatus.IsInFightingStatus()
    end)
    if okFight and fighting then return true end
    local ok, gs = pcall(function()
        return slua_GameFrontendHUD and slua_GameFrontendHUD:GetGameState()
    end)
    if ok and gs then
        local state = gs.GameModeState
        if not state and gs.GetGameModeState then state = gs:GetGameModeState() end
        if state and state ~= 'Lobby' and state ~= 'None' and state ~= '' then return true end
    end
    return false
end
_G.isInPlayableGame = isInPlayableGame

local function getCharacterAvatarComponent(uCharacter)
    if not uCharacter or not slua.isValid(uCharacter) then return nil end
    if uCharacter.CharacterAvatarComp2_BP and slua.isValid(uCharacter.CharacterAvatarComp2_BP) then
        return uCharacter.CharacterAvatarComp2_BP
    end
    if uCharacter.AvatarComponent2 and slua.isValid(uCharacter.AvatarComponent2) then
        return uCharacter.AvatarComponent2
    end
    if uCharacter.AvatarComponent and slua.isValid(uCharacter.AvatarComponent) then
        return uCharacter.AvatarComponent
    end
    if uCharacter.CharacterAvatarComp and slua.isValid(uCharacter.CharacterAvatarComp) then
        return uCharacter.CharacterAvatarComp
    end
    return nil
end

local function findAvatarSlotIndex(ApplyData, slotId)
    for i = 0, ApplyData:Num() - 1 do
        local eq = ApplyData:Get(i)
        if eq and eq.SlotID == slotId then return i end
    end
    return nil
end

local function ensureAvatarSlot(ApplyData, slotId)
    local idx = findAvatarSlotIndex(ApplyData, slotId)
    if idx then return idx end
    ApplyData:Add({ SlotID = slotId, ItemId = 0, AdditionalItemID = 0 })
    return ApplyData:Num() - 1
end

local function cacheDownloadItem(itemId)
    if not itemId or itemId == 0 then return end
    if not _G.skinIdCache[itemId] then
        pcall(_G.download_item, itemId)
        _G.skinIdCache[itemId] = true
    end
end

local function applyAvatarSlotSkin(ApplyData, slotId, targetSkinId, additionalItemId)
    if not targetSkinId or targetSkinId == 0 then return false end
    local idx = ensureAvatarSlot(ApplyData, slotId)
    local equipment = ApplyData:Get(idx)
    if not equipment then return false end
    local extra = additionalItemId or equipment.AdditionalItemID or 0
    if equipment.ItemId == targetSkinId and (not additionalItemId or equipment.AdditionalItemID == additionalItemId) then
        return false
    end
    cacheDownloadItem(targetSkinId)
    equipment.ItemId = targetSkinId
    if additionalItemId and additionalItemId > 0 then
        equipment.AdditionalItemID = additionalItemId
    end
    ApplyData:Set(idx, equipment)
    return true
end

local function refreshAvatarComp(avatarComp)
    pcall(function() avatarComp:OnRep_BodySlotStateChanged() end)
    pcall(function() avatarComp:OnAvatarMeshChanged() end)
    pcall(function()
        if avatarComp.RefreshAvatarDisplay then avatarComp:RefreshAvatarDisplay() end
    end)
    pcall(function()
        if avatarComp.UpdateAvatarDisplay then avatarComp:UpdateAvatarDisplay() end
    end)
    pcall(function()
        if avatarComp.ForceUpdateCharacterAppearance then avatarComp:ForceUpdateCharacterAppearance() end
    end)
end

local function tryAvatarCompEquip(avatarComp, slotId, skinId, additionalId)
    if not avatarComp or not skinId or skinId == 0 then return false end
    cacheDownloadItem(skinId)
    local changed = false
    pcall(function()
        if avatarComp.PutOnEquipmentBySlot then
            avatarComp:PutOnEquipmentBySlot(slotId, skinId, additionalId or 0)
            changed = true
        end
    end)
    pcall(function()
        if avatarComp.PutOnEquipment then
            avatarComp:PutOnEquipment(skinId, slotId, additionalId or 0)
            changed = true
        end
    end)
    pcall(function()
        if avatarComp.ChangeItemAvatar then
            avatarComp:ChangeItemAvatar(skinId, slotId, additionalId or 0)
            changed = true
        end
    end)
    pcall(function()
        if slotId == _G.CustSlotType.BackpackEquipemtSlot or slotId == _G.CustSlotType.HelmetEquipemtSlot then
            if avatarComp.UpdateEquipmentBySlot then
                avatarComp:UpdateEquipmentBySlot(slotId, skinId, additionalId or 0)
                changed = true
            end
            if avatarComp.SetEquipmentSlotData then
                avatarComp:SetEquipmentSlotData(slotId, skinId, additionalId or 0)
                changed = true
            end
        end
    end)
    return changed
end

local function readShirtFromConfigIni()
    for _, base in ipairs(loadSkinDataBases()) do
        local f = io.open(base .. '/config.ini', 'r')
        if f then
            local txt = f:read('*all') or ''
            f:close()
            local id = tonumber(txt:match('[%r\n]SHIRT=(%d+)')) or tonumber(txt:match('^SHIRT=(%d+)'))
            if id and id > 0 then return id end
        end
    end
    return 0
end

function _G.invalidateMatchOutfitCache(reason)
    _G.__matchOutfitOk = nil
    _G.__lastAppliedMatchOutfit = nil
    _G.__matchOutfitApplyLogged = nil
    _G.__lastGearAvatarTick = nil
    _G.__outfitProbeDone = nil
    _G.__matchOutfitProbeDone = nil
    _G.__outfitZeroLogged = nil
    _G.__matchCharMissLogged = nil
    _G.__avatarCompMissLogged = nil
    _G.__lastOutfitFailLog = nil
    _G.__outfitRetryChain = nil
    if reason then
        local now = os.clock()
        if _G.__lastOutfitCacheReason ~= reason or not _G.__lastOutfitCacheLog
            or (now - _G.__lastOutfitCacheLog) > 3 then
            _G.__lastOutfitCacheReason = reason
            _G.__lastOutfitCacheLog = now
            skinProbeLog('outfit cache cleared: ' .. tostring(reason))
        end
    end
end

local function resolveSuitSkinForAvatar()
    if isInPlayableGame() or isInActiveMatch() then
        pcall(function() if _G.ReadConfigFile then _G.ReadConfigFile(true) end end)
    end
    local suitSkin = tonumber(_G.AddOutfitLastLobbyOutfitRes) or tonumber(_G.__ACTIVE_OUTFIT_RES) or tonumber(_G.SuitSkin) or 0
    if _G.isLobbyOutfitRes and not _G.isLobbyOutfitRes(suitSkin) then suitSkin = 0 end
    if suitSkin <= 0 then
        suitSkin = readShirtFromConfigIni()
        if _G.isLobbyOutfitRes and not _G.isLobbyOutfitRes(suitSkin) then suitSkin = 0 end
    end
    if suitSkin > 0 then
        _G.__ACTIVE_OUTFIT_RES = suitSkin
        _G.SuitSkin = suitSkin
    end
    return suitSkin
end
_G.resolveMatchOutfitRes = resolveSuitSkinForAvatar

local function resolveOutfitInsId(suitSkin)
    suitSkin = tonumber(suitSkin) or 0
    if suitSkin <= 0 then return 0 end
    local ins = tonumber(_G.AddOutfitLastLobbyOutfitIns) or 0
    if ins > 0 then return ins end
    if _G.getOutfitInsId then
        return tonumber(_G.getOutfitInsId(suitSkin)) or 0
    end
    return 0
end

local function readClothesSlotRes(avatarComp, slots)
    if not avatarComp or not slua.isValid(avatarComp) then return 0 end
    local worn = 0
    pcall(function()
        local netData = avatarComp.NetAvatarData
        local applyData = netData and netData.SlotSyncData
        if applyData and slua.isValid(applyData) then
            local idx = findAvatarSlotIndex(applyData, slots.ClothesEquipemtSlot)
            if idx then
                local eq = applyData:Get(idx)
                if eq then worn = tonumber(eq.ItemId) or 0 end
            end
        end
    end)
    return worn
end

local function applySuitToCharacter(avatarComp, ApplyData, suitSkin, slots, outfitIns)
    if not suitSkin or suitSkin <= 0 then return false end
    outfitIns = tonumber(outfitIns) or resolveOutfitInsId(suitSkin)
    local changed = false
    cacheDownloadItem(suitSkin)
    pcall(function() if _G.download_item then _G.download_item(suitSkin) end end)
    if avatarComp then
        local addId = outfitIns > 0 and outfitIns or 0
        changed = tryAvatarCompEquip(avatarComp, slots.ClothesEquipemtSlot, suitSkin, addId) or changed
        pcall(function()
            if avatarComp.PutOnCustomEquipmentByID then
                avatarComp:PutOnCustomEquipmentByID(suitSkin)
                changed = true
            end
        end)
        pcall(function()
            if avatarComp.HandleEquipItem then
                avatarComp:HandleEquipItem(FItemDefineID(4, suitSkin), FAvatarCustomDefault())
                changed = true
            end
        end)
        if outfitIns > 0 then
            pcall(function()
                if avatarComp.PutOnEquipmentBySpecialID then
                    avatarComp:PutOnEquipmentBySpecialID(suitSkin, outfitIns)
                    changed = true
                end
            end)
        end
    end
    if ApplyData and slua.isValid(ApplyData) then
        changed = applyAvatarSlotSkin(ApplyData, slots.ClothesEquipemtSlot, suitSkin, outfitIns > 0 and outfitIns or nil) or changed
    end
    return changed
end

function _G.applySuitToMatchComp(avatarComp, suitSkin)
    if not avatarComp or not slua.isValid(avatarComp) then return false end
    suitSkin = tonumber(suitSkin) or 0
    if suitSkin <= 0 then return false end
    local outfitIns = resolveOutfitInsId(suitSkin)
    local slots = _G.CustSlotType
    local netData = avatarComp.NetAvatarData
    local ApplyData = netData and slua.isValid(netData) and netData.SlotSyncData
    local changed = applySuitToCharacter(avatarComp, ApplyData, suitSkin, slots, outfitIns)
    refreshAvatarComp(avatarComp)
    if readClothesSlotRes(avatarComp, slots) == suitSkin then
        return true
    end
    return changed
end

local function scheduleMatchOutfitRetry(suitSkin, attempt)
    if isInLobby() or not isInPlayableGame() then
        _G.__outfitRetryChain = nil
        return
    end
    attempt = tonumber(attempt) or 1
    if attempt > 20 then
        _G.__outfitRetryChain = nil
        return
    end
    if _G.__matchOutfitOk and _G.__lastAppliedMatchOutfit == suitSkin then
        _G.__outfitRetryChain = nil
        return
    end
    if _G.__outfitRetryChain == suitSkin and attempt == 1 then return end
    _G.__outfitRetryChain = suitSkin
    if not _G.Mytimer_ticker or not _G.Mytimer_ticker.AddTimerOnce then return end
    pcall(function()
        _G.Mytimer_ticker.AddTimerOnce(1.0 + (attempt * 0.5), function()
            if not isInPlayableGame() then
                _G.__outfitRetryChain = nil
                return
            end
            if _G.applyMatchOutfitNow and _G.applyMatchOutfitNow(true) then
                _G.__outfitRetryChain = nil
                return
            end
            scheduleMatchOutfitRetry(suitSkin, attempt + 1)
        end)
    end)
end

local function isLocalOutfitTarget(uCharacter)
    if not uCharacter or not slua.isValid(uCharacter) then return false end
    local isSelf = false
    pcall(function()
        if uCharacter.IsSelf and uCharacter:IsSelf() then isSelf = true end
        if uCharacter.bIsSelf then isSelf = true end
        if uCharacter.IsLocalPlayer and uCharacter:IsLocalPlayer() then isSelf = true end
    end)
    if isSelf then return true end
    local localChar = getSkinPlayerCharacter()
    return localChar and uCharacter == localChar
end
_G.isLocalOutfitTarget = isLocalOutfitTarget

local function collectOutfitTargets()
    local out, seen = {}, {}
    local function add(ch)
        if not ch or not slua.isValid(ch) then return end
        if not isLocalOutfitTarget(ch) then return end
        local key = tostring(ch)
        if seen[key] then return end
        seen[key] = true
        table.insert(out, ch)
    end
    add(getSkinPlayerCharacter())
    pcall(function()
        local pc = getSkinPlayerController()
        add(getCharacterFromController(pc))
    end)
    pcall(function()
        local m = require("client.logic.avatar.logic_team_avatar_manager")
        if m then
            if m.GetModelActor then add(m.GetModelActor()) end
            if m.GetShowingModel then add(m:GetShowingModel()) end
            if m.GetTeamAvatarActor then add(m:GetTeamAvatarActor()) end
            if m.GetMainAvatarActor then add(m:GetMainAvatarActor()) end
        end
    end)
    pcall(function()
        local UIUtil = require("client.common.ui_util")
        local gi = UIUtil and UIUtil.GetGameInstance and UIUtil.GetGameInstance()
        if not gi then return end
        local UGameplayStatics = import("GameplayStatics")
        if not UGameplayStatics then return end
        local uActor = import("Actor")
        for _, className in ipairs({ "STExtraLobbyCharacter", "UAECharacter", "STExtraBaseCharacter" }) do
            pcall(function()
                local cls = import(className)
                if cls then
                    local arr = UGameplayStatics.GetAllActorsOfClass(
                        gi, cls, slua.Array(UEnums.EPropertyClass.Object, uActor))
                    if arr then
                        for _, actor in pairs(arr) do
                            add(actor)
                        end
                    end
                end
            end)
        end
    end)
    pcall(function()
        local ModuleManager = require("client.module_framework.ModuleManager")
        if not ModuleManager or not ModuleManager.LobbyModuleConfig then return end
        local cfg = ModuleManager.LobbyModuleConfig
        for _, key in ipairs({ "LobbyModel", "logic_lobby_main", "LobbyAvatar" }) do
            local modName = cfg[key]
            if modName then
                local mod = ModuleManager.GetModule(modName)
                if mod then
                    if mod.GetShowModel then add(mod:GetShowModel()) end
                    if mod.GetLobbyModel then add(mod:GetLobbyModel()) end
                    if mod.GetModelActor then add(mod:GetModelActor()) end
                end
            end
        end
    end)
    return out
end
_G.collectOutfitTargets = collectOutfitTargets
_G.getCharacterFromController = getCharacterFromController
_G.getSkinPlayerCharacter = getSkinPlayerCharacter

local function applyOutfitViaWardrobe(itemId)
    if not itemId or itemId == 0 then return false end
    cacheDownloadItem(itemId)
    local ok = false
    pcall(function()
        local ModuleManager = require("client.module_framework.ModuleManager")
        if not ModuleManager or not ModuleManager.CommonModuleConfig then return end
        local cfg = ModuleManager.CommonModuleConfig
        for _, key in ipairs({ "logic_wardrobe", "logic_wardrobe_new", "WardrobeLogic" }) do
            local modName = cfg[key]
            if modName then
                local mod = ModuleManager.GetModule(modName)
                if mod then
                    if mod.PutOnEquipment then mod:PutOnEquipment(itemId); ok = true end
                    if mod.putOnEquipment then mod:putOnEquipment(itemId); ok = true end
                    if mod.ForcePutOnEquipment then mod:ForcePutOnEquipment(itemId); ok = true end
                end
            end
        end
    end)
    return ok
end
_G.applyOutfitViaWardrobe = applyOutfitViaWardrobe

function _G.equip_character_avatar(uCharacter)
    if not uCharacter or not slua.isValid(uCharacter) then return false end
    local inMatch = isInPlayableGame()
    if inMatch then
        local localChar = getMatchPlayerCharacter()
        if localChar and uCharacter ~= localChar then
            local isSelf = false
            pcall(function()
                if uCharacter.IsSelf and uCharacter:IsSelf() then isSelf = true end
                if uCharacter.bIsSelf then isSelf = true end
            end)
            if not isSelf then return false end
        end
    elseif not isLocalOutfitTarget(uCharacter) then
        return false
    end
    local avatarComp = getCharacterAvatarComponent(uCharacter)
    if not avatarComp then
        if inMatch and not isInLobby() and not _G.__avatarCompMissLogged then
            _G.__avatarCompMissLogged = true
            skinProbeLog('outfit: no avatarComp on match character')
        end
        return false
    end

    local netData = avatarComp.NetAvatarData
    local ApplyData = netData and slua.isValid(netData) and netData.SlotSyncData
    local BackpackUtils = import("BackpackUtils")
    local changed = false
    local slots = _G.CustSlotType
    local suitSkin = resolveSuitSkinForAvatar()
    if suitSkin <= 0 and inMatch and not _G.__outfitZeroLogged then
        _G.__outfitZeroLogged = true
        skinProbeLog('outfit missing: set SHIRT in config or select outfit in lobby')
    end

    if suitSkin > 0 and not inMatch then
        changed = applySuitToCharacter(avatarComp, ApplyData, suitSkin, slots) or changed
        pcall(function()
            if GameStatus and GameStatus.IsInLobbyOrMainCity and GameStatus.IsInLobbyOrMainCity() then
                applyOutfitViaWardrobe(suitSkin)
            end
        end)
    end
    if _G.HatSkin and _G.HatSkin > 0 then
        changed = tryAvatarCompEquip(avatarComp, slots.HatEquipemtSlot, _G.HatSkin, 0) or changed
    end
    if _G.FaceSkin and _G.FaceSkin > 0 then
        changed = tryAvatarCompEquip(avatarComp, slots.FaceEquipemtSlot, _G.FaceSkin, 0) or changed
    end
    if _G.MaskSkin and _G.MaskSkin > 0 then
        changed = tryAvatarCompEquip(avatarComp, slots.FaceEquipemtSlot, _G.MaskSkin, 0) or changed
    end
    if _G.GlovesSkin and _G.GlovesSkin > 0 then
        changed = tryAvatarCompEquip(avatarComp, slots.HandEffectEquipemtSlot, _G.GlovesSkin, 0) or changed
    end
    if _G.PantSkin and _G.PantSkin > 0 then
        changed = tryAvatarCompEquip(avatarComp, slots.PantsEquipemtSlot, _G.PantSkin, 0) or changed
    end
    if _G.ShoeSkin and _G.ShoeSkin > 0 then
        changed = tryAvatarCompEquip(avatarComp, slots.ShoesEquipemtSlot, _G.ShoeSkin, 0) or changed
    end
    if _G.GliderSkin and _G.GliderSkin > 0 then
        changed = tryAvatarCompEquip(avatarComp, slots.GlideEquipemtSlot, _G.GliderSkin, 0) or changed
    end
    if _G.ParachuteSkin and _G.ParachuteSkin > 0 then
        changed = tryAvatarCompEquip(avatarComp, slots.ParachuteEquipemtSlot, _G.ParachuteSkin, 0) or changed
    end

    local bagSkin, bagBase, bagLvl, helmSkin, helmBase, helmLvl = resolveGearVisualState(
        uCharacter, ApplyData, BackpackUtils, slots, inMatch)

    if bagSkin and bagSkin > 0 and bagLvl then
        changed = tryAvatarCompEquip(avatarComp, slots.BackpackEquipemtSlot, bagSkin, bagBase) or changed
    end
    if helmSkin and helmSkin > 0 and helmLvl then
        changed = tryAvatarCompEquip(avatarComp, slots.HelmetEquipemtSlot, helmSkin, helmBase) or changed
    end

    if ApplyData and slua.isValid(ApplyData) then
        changed = applyAvatarSlotSkin(ApplyData, slots.HatEquipemtSlot, _G.HatSkin) or changed
        changed = applyAvatarSlotSkin(ApplyData, slots.FaceEquipemtSlot, _G.FaceSkin) or changed
        if _G.MaskSkin and _G.MaskSkin ~= 0 and _G.MaskSkin ~= _G.FaceSkin then
            changed = applyAvatarSlotSkin(ApplyData, slots.FaceEquipemtSlot, _G.MaskSkin) or changed
        end
        changed = applyAvatarSlotSkin(ApplyData, slots.HandEffectEquipemtSlot, _G.GlovesSkin) or changed
        changed = applyAvatarSlotSkin(ApplyData, slots.PantsEquipemtSlot, _G.PantSkin) or changed
        changed = applyAvatarSlotSkin(ApplyData, slots.ShoesEquipemtSlot, _G.ShoeSkin) or changed
        changed = applyAvatarSlotSkin(ApplyData, slots.GlideEquipemtSlot, _G.GliderSkin) or changed
        changed = applyAvatarSlotSkin(ApplyData, slots.ParachuteEquipemtSlot, _G.ParachuteSkin) or changed
        if bagSkin and bagSkin > 0 and bagLvl then
            changed = applyAvatarSlotSkin(ApplyData, slots.BackpackEquipemtSlot, bagSkin, bagBase) or changed
        end
        if helmSkin and helmSkin > 0 and helmLvl then
            changed = applyAvatarSlotSkin(ApplyData, slots.HelmetEquipemtSlot, helmSkin, helmBase) or changed
        end
    end

    if suitSkin > 0 then
        changed = applySuitToCharacter(avatarComp, ApplyData, suitSkin, slots, resolveOutfitInsId(suitSkin)) or changed
    end

    if changed then refreshAvatarComp(avatarComp) end
    if inMatch then
        if not _G.__matchOutfitProbeDone then
            _G.__matchOutfitProbeDone = true
            pcall(function()
                skinProbeLog('outfit shirt=' .. tostring(suitSkin)
                    .. ' bag=' .. tostring(bagSkin) .. ' bagLvl=' .. tostring(bagLvl)
                    .. ' helm=' .. tostring(helmSkin) .. ' helmLvl=' .. tostring(helmLvl)
                    .. ' inMatch=true'
                    .. ' changed=' .. tostring(changed)
                    .. ' hasComp=' .. tostring(avatarComp ~= nil))
            end)
        end
    elseif not _G.__outfitProbeDone then
        _G.__outfitProbeDone = true
        pcall(function()
            skinProbeLog('outfit shirt=' .. tostring(suitSkin)
                .. ' bag=' .. tostring(bagSkin) .. ' bagLvl=' .. tostring(bagLvl)
                .. ' helm=' .. tostring(helmSkin) .. ' helmLvl=' .. tostring(helmLvl)
                .. ' inMatch=false'
                .. ' changed=' .. tostring(changed)
                .. ' hasComp=' .. tostring(avatarComp ~= nil))
        end)
    end
    return changed
end

function _G.applyMatchOutfitNow(force)
    if isInLobby() then return false end
    local inMatch = isInPlayableGame()
    if not inMatch then return false end
    local suitSkin = resolveSuitSkinForAvatar()
    if suitSkin <= 0 then
        local now = os.clock()
        if not _G.__lastOutfitFailLog or (now - _G.__lastOutfitFailLog) > 15 then
            _G.__lastOutfitFailLog = now
            skinProbeLog('applyMatchOutfitNow: no suitSkin (SHIRT=0 in config?)')
        end
        return false
    end
    if not force and _G.__matchOutfitOk and _G.__lastAppliedMatchOutfit == suitSkin then
        return true
    end
    if _G.__lastAppliedMatchOutfit and _G.__lastAppliedMatchOutfit ~= suitSkin then
        pcall(function() _G.invalidateMatchOutfitCache('outfit res changed') end)
    end
    local outfitIns = resolveOutfitInsId(suitSkin)
    if outfitIns > 0 then _G.AddOutfitLastLobbyOutfitIns = outfitIns end
    local char = getMatchPlayerCharacter()
    if not char then
        scheduleMatchOutfitRetry(suitSkin, 1)
        local now = os.clock()
        if not _G.__lastOutfitFailLog or (now - _G.__lastOutfitFailLog) > 15 then
            _G.__lastOutfitFailLog = now
            skinProbeLog('applyMatchOutfitNow: no character shirt=' .. suitSkin)
        end
        return false
    end
    pcall(function() if _G.download_item then _G.download_item(suitSkin) end end)
    local comp = getCharacterAvatarComponent(char)
    if not comp and char.CharacterAvatarComp2_BP and slua.isValid(char.CharacterAvatarComp2_BP) then
        comp = char.CharacterAvatarComp2_BP
    end
    local suitOk = false
    local worn = 0
    if comp and slua.isValid(comp) then
        suitOk = _G.applySuitToMatchComp(comp, suitSkin)
        worn = readClothesSlotRes(comp, _G.CustSlotType)
    end
    pcall(function() _G.equip_character_avatar(char) end)
    if suitOk then
        _G.__lastAppliedMatchOutfit = suitSkin
        _G.__matchOutfitOk = true
        skinProbeLog('match outfit apply shirt=' .. suitSkin
            .. ' ins=' .. tostring(outfitIns)
            .. ' slot=' .. tostring(worn) .. ' ok=true')
    else
        scheduleMatchOutfitRetry(suitSkin, 1)
        local now = os.clock()
        if not _G.__lastOutfitFailLog or (now - _G.__lastOutfitFailLog) > 8 then
            _G.__lastOutfitFailLog = now
            skinProbeLog('applyMatchOutfitNow: suit apply failed shirt=' .. suitSkin
                .. ' ins=' .. tostring(outfitIns)
                .. ' hasComp=' .. tostring(comp ~= nil)
                .. ' slot=' .. tostring(worn))
        end
    end
    return suitOk
end

function _G.applyMatchOutfitIfNeeded(force)
    return _G.applyMatchOutfitNow(force)
end

function _G.HandlePetLogic()
    pcall(function()
        if not _G.PetSkin or _G.PetSkin == 0 or _G.PetSkin == 50000 then return end
        if _G.PetSkin == _G.LastAppliedPet then
            _G.__petRetryTick = (_G.__petRetryTick or 0) + 1
            local every = isInActiveMatch() and 8 or 4
            if _G.__petRetryTick % every ~= 0 then return end
        end
        cacheDownloadItem(_G.PetSkin)

        local ModuleManager = require("client.module_framework.ModuleManager")
        if ModuleManager and ModuleManager.CommonModuleConfig then
            local logic_pet = ModuleManager.GetModule(ModuleManager.CommonModuleConfig.logic_pet)
            if logic_pet then
                if logic_pet.SetCurPetID then logic_pet:SetCurPetID(_G.PetSkin) end
                if logic_pet.EquipPet then logic_pet:EquipPet(_G.PetSkin) end
                if logic_pet.CreatePet then logic_pet:CreatePet(_G.PetSkin) end
                if logic_pet.ShowPet then logic_pet:ShowPet(_G.PetSkin) end
            end
        end

        local pc = getSkinPlayerController()
        if pc and slua.isValid(pc) then
            if pc.InitialPetInfo then pc.InitialPetInfo.PetId = _G.PetSkin end
            if pc.PetComponent and slua.isValid(pc.PetComponent) then
                if pc.PetComponent.SetPetID then pc.PetComponent:SetPetID(_G.PetSkin) end
                if pc.PetComponent.CreatePet then pc.PetComponent:CreatePet(_G.PetSkin) end
            end
        end
        _G.LastAppliedPet = _G.PetSkin
    end)
end

function _G.DeadBox_TemperRequest(PlayerController)
    if not _G.WeaponSkinIndex or not next(_G.WeaponSkinIndex) then return end
    local uCharacter = PlayerController:GetPlayerCharacterSafety()
    if not uCharacter then return end
    
    local UGameplayStatics = import("GameplayStatics")
    if UGameplayStatics then
        local uActor = import("Actor")
        local UIUtil = require("client.common.ui_util")
        if UIUtil then
            local uGameInstance = UIUtil.GetGameInstance()
            if uGameInstance then
                local APlayerTombBox = import("PlayerTombBox")
                local uActorArray = UGameplayStatics.GetAllActorsOfClass(uGameInstance, APlayerTombBox, slua.Array(UEnums.EPropertyClass.Object, uActor))
                for _, actor in pairs(uActorArray) do
                    if _G.IsPtrValid(actor) then
                        local DamageCauser = actor.DamageCauser
                        if DamageCauser and DamageCauser.Playerkey == PlayerController.Playerkey then
                            local Deadboxavatar = actor.DeadBoxAvatarComponent_BP
                            if Deadboxavatar and not table.contains(_G.AlreadyChangedSet, actor) then
                                local actorLocation = actor:K2_GetActorLocation()
                                local found = false
                                for _, entry in pairs(_G.DeadBoxSkins) do
                                    if locationsClose(entry.location, actorLocation, 1.0) then
                                        Deadboxavatar:ResetItemAvatar()
                                        Deadboxavatar:PreChangeItemAvatar(entry.SkinID)
                                        Deadboxavatar:SyncChangeItemAvatar(entry.SkinID)
                                        table.insert(_G.AlreadyChangedSet, actor)
                                        found = true
                                        break
                                    end
                                end
                                if not found then
                                    local ApplySkinID = 0
                                    local CurrentVehicle = uCharacter.CurrentVehicle
                                    if CurrentVehicle and _G.CurrentEquipVehicleID ~= 0 then
                                        ApplySkinID = tostring(_G.CurrentEquipVehicleID) .. "1"
                                    else
                                        local currweapon = uCharacter:GetCurrentWeapon()
                                        if currweapon then
                                            ApplySkinID = slua.IndexReference(currweapon.synData:Get(7), "defineID").TypeSpecificID
                                        end
                                    end
                                    if ApplySkinID and ApplySkinID ~= 0 then
                                        Deadboxavatar:ResetItemAvatar()
                                        Deadboxavatar:PreChangeItemAvatar(ApplySkinID)
                                        Deadboxavatar:SyncChangeItemAvatar(ApplySkinID)
                                        table.insert(_G.DeadBoxSkins, { location = actorLocation, SkinID = ApplySkinID })
                                        table.insert(_G.AlreadyChangedSet, actor)
                                    end
                                end
                            end
                        end
                    end
                end
            end
        end
    end
end

function _G.GameAvatarHandlerDeadBox()
    local PlayerController = slua_GameFrontendHUD and slua_GameFrontendHUD:GetPlayerController()
    if PlayerController then
        _G.DeadBox_TemperRequest(PlayerController)
    end
end

function _G.GameAvatarHandlerplayers()
    local inGame = isInPlayableGame()
    local desiredOutfit = inGame and resolveSuitSkinForAvatar() or 0
    local gearStateKey = nil
    if inGame then
        local ch = getMatchPlayerCharacter()
        if ch and slua.isValid(ch) then
            local BU = import("BackpackUtils")
            local bL = getCharacterBattleGearLevel(ch, "bag", BU)
            if not bL then bL = getGearLevelFromInventory(ch, "bag", BU) end
            local hL = getCharacterBattleGearLevel(ch, "helmet", BU)
            if not hL then hL = getGearLevelFromInventory(ch, "helmet", BU) end
            gearStateKey = tostring(bL) .. ":" .. tostring(hL)
        end
    end
    if inGame and desiredOutfit > 0
        and _G.__lastAppliedMatchOutfit
        and _G.__lastAppliedMatchOutfit ~= desiredOutfit then
        pcall(function() _G.invalidateMatchOutfitCache('lobby outfit changed') end)
        pcall(function() _G.applyMatchOutfitNow(true) end)
    elseif inGame and _G.__matchOutfitOk and desiredOutfit > 0
        and _G.__lastAppliedMatchOutfit == desiredOutfit then
        local now = os.clock()
        local sameGear = gearStateKey and gearStateKey == _G.__lastGearStateKey
        if sameGear and _G.__lastGearAvatarTick and (now - _G.__lastGearAvatarTick) < 15 then
            pcall(_G.HandlePetLogic)
            return
        end
        _G.__lastGearAvatarTick = now
        _G.__lastGearStateKey = gearStateKey
    end
    local targets = {}
    local inMatch = isInPlayableGame()
    if inMatch then
        local ch = getMatchPlayerCharacter()
        if ch and slua.isValid(ch) then
            targets = { ch }
        end
    else
        targets = collectOutfitTargets()
        if #targets == 0 then
            local ch = getSkinPlayerCharacter()
            if ch and slua.isValid(ch) then
                targets = { ch }
            end
        end
    end
    for _, uChar in ipairs(targets) do
        pcall(function() _G.equip_character_avatar(uChar) end)
    end
    _G.HandlePetLogic()
end

function _G.Game_Vehicle_Avatar_Change(uCharacter)
    pcall(function()
        local CurrentVehicle = uCharacter.CurrentVehicle
        if CurrentVehicle then
            local VehicleAvatar = CurrentVehicle.VehicleAvatar
            if VehicleAvatar then
                VehicleAvatar.curSwitchEffectId = 7303001
                local DefaultAvatarID = tostring(VehicleAvatar:GetDefaultAvatarID())
                local CurrentAvatarID = CurrentVehicle:GetAvatarId()
                for gunId, skinIdTable in pairs(_G.VehskinIdMappings or {}) do
                    local gunIdStr = tostring(gunId)
                    if DefaultAvatarID:find(gunIdStr) then
                        local skinId = skinIdTable[1]
                        if CurrentAvatarID ~= skinId then
                            VehicleAvatar:ChangeItemAvatar(skinId, true)
                            _G.CurrentEquipVehicleID = skinId
                            break
                        end
                    end
                end
            end
        end
    end)
end

function _G.GameAvatarHandlervehicles()
    local PlayerController = slua_GameFrontendHUD and slua_GameFrontendHUD:GetPlayerController()
    if PlayerController then
        local uCharacter = PlayerController:GetPlayerCharacterSafety()
        if uCharacter and _G.Game_Vehicle_Avatar_Change then
            _G.Game_Vehicle_Avatar_Change(uCharacter)
        end
    end
end

function _G.ApplyLobbyTheme()
    if isInActiveMatch() then return end
    pcall(function()
        local themeID = _G.TargetLobbyThemeID
        if not themeID or themeID == 0 then return end
        if _G.LastAppliedThemeID == themeID then return end
        cacheDownloadItem(themeID)
        local ModuleManager = require('client.module_framework.ModuleManager')
        if not ModuleManager then return end
        local LobbyThemeManager = ModuleManager.GetModule(ModuleManager.LobbyModuleConfig.LobbyThemeManager)
        if LobbyThemeManager then
            local ok = false
            if LobbyThemeManager.ShowThemeByItemID then
                pcall(function() LobbyThemeManager:ShowThemeByItemID(themeID) end)
                ok = true
            end
            if LobbyThemeManager.SetTheme then
                pcall(function() LobbyThemeManager:SetTheme(themeID) end)
                ok = true
            end
            if LobbyThemeManager.ApplyTheme then
                pcall(function() LobbyThemeManager:ApplyTheme(themeID) end)
                ok = true
            end
            if ok then _G.LastAppliedThemeID = themeID end
        end
    end)
end

function _G.CheckLobbyThemeChanges()
    pcall(function()
        local oldID = _G.TargetLobbyThemeID
        _G.ReadConfigFile(true)
        if _G.TargetLobbyThemeID ~= oldID then
            _G.LastAppliedThemeID = nil
            _G.ApplyLobbyTheme()
        end
    end)
end

function _G.TryShowWelcome()
    pcall(function()
        local CommonMsgBoxMgr = require("client.slua.logic.common.logic_common_msg_box")
        if not CommonMsgBoxMgr then return end
        local activeStatus = "Anubis Menu & Status\n"
        activeStatus = activeStatus .. "\nWeapon Skins: Active"
        activeStatus = activeStatus .. "\nKill Counter: Active"
        activeStatus = activeStatus .. "\nOutfit Skins: Active"
        activeStatus = activeStatus .. "\nLobby Theme: Active"
        activeStatus = activeStatus .. "\nDeadBox Skins: Active"
        activeStatus = activeStatus .. "\nVehicle Skins: Active"
        activeStatus = activeStatus .. "\n\nConfigure your values in config.ini and changes will apply automatically without UI hooks.\n\nEnjoy Anubis!"
        CommonMsgBoxMgr.Show(1, "Anubis MENU", activeStatus, function() end)
        _G.WelcomeShown = true
    end)
end

function _G.GetKillCounterPath()
    for _, base in ipairs(loadSkinDataBases()) do
        local path = base .. '/NumberUpdate.txt'
        local file = io.open(path, 'r')
        if file then file:close() return path end
    end
    for _, base in ipairs(loadSkinDataBases()) do
        local f = io.open(base .. '/config.ini', 'r')
        if f then f:close() return base .. '/NumberUpdate.txt' end
    end
    return '/storage/emulated/0/Android/data/com.pubg.imobile/files/NumberUpdate.txt'
end

_G.ActiveKillCounterPath = nil

local function saveKillCountToFile()
    if not _G.ActiveKillCounterPath then _G.ActiveKillCounterPath = _G.GetKillCounterPath() end
    local file = io.open(_G.ActiveKillCounterPath, 'w+')
    if not file then return end
    local content = '{\n'
    for weaponID, count in pairs(_G.killCountInfo) do
        content = content .. string.format('    [%d] = %d,\n', weaponID, count)
    end
    content = content .. '}'
    file:write(content)
    file:close()
    _G.lastFileContent = content
end

function _G.loadKillCountFromFile()
    if not _G.ActiveKillCounterPath then _G.ActiveKillCounterPath = _G.GetKillCounterPath() end
    local file = io.open(_G.ActiveKillCounterPath, 'r')
    if file then
        local content = file:read('*a')
        file:close()
        _G.lastFileContent = content
        if content ~= '' then
            content = content:gsub('\239\187\191', ''):gsub('^%s+', '')
            local tempTable = {}
            for weaponID, count in content:gmatch('%[(%d+)%]%s*=%s*(%d+)') do
                tempTable[tonumber(weaponID)] = tonumber(count)
            end
            if next(tempTable) then _G.killCountInfo = tempTable end
        end
    end
end

function _G.getKills(weaponID)
    return weaponID and _G.killCountInfo[weaponID] or 0
end

function _G.addKill(weaponID, count)
    if not weaponID or not count then return end
    local currentTime = os.clock()
    if _G.LastKillTime[weaponID] and (currentTime - _G.LastKillTime[weaponID]) < 0.5 then return end
    _G.LastKillTime[weaponID] = currentTime
    _G.killCountInfo[weaponID] = (_G.killCountInfo[weaponID] or 0) + count
    pcall(saveKillCountToFile)
    local PlayerController = slua_GameFrontendHUD and slua_GameFrontendHUD:GetPlayerController()
    if PlayerController then
        local uCharacter = PlayerController:GetPlayerCharacterSafety()
        if uCharacter then
            local currweapon = uCharacter:GetCurrentWeapon()
            if currweapon then
                local SkinID = slua.IndexReference(currweapon.synData:Get(7), "defineID").TypeSpecificID
                if _G.OurkillCountSystem then
                    _G.OurkillCountSystem:UpdateMainKillCounterUI(true, weaponID, SkinID)
                end
            end
        end
    end
end

function _G.ForceUpdateKillCounterUI()
    pcall(function()
        local PlayerController = slua_GameFrontendHUD and slua_GameFrontendHUD:GetPlayerController()
        if not PlayerController or not slua.isValid(PlayerController) then return end
        local uCharacter = PlayerController:GetPlayerCharacterSafety()
        if not uCharacter or not slua.isValid(uCharacter) then return end
        local currweapon = uCharacter:GetCurrentWeapon()
        if not currweapon or not slua.isValid(currweapon) then return end
        local DefineID = currweapon:GetItemDefineID() and currweapon:GetItemDefineID().TypeSpecificID or 0
        if DefineID == 0 then return end
        local currentEquipAvatarID = slua.IndexReference(currweapon.synData:Get(7), "defineID").TypeSpecificID
        local UIManager = require("client.slua_ui_framework.manager")
        local MainKillCounter = UIManager.GetUI(UIManager.UI_Config_InGame.MainKillCounter)
        if MainKillCounter and slua.isValid(MainKillCounter) then
            local ModuleManager = require("client.module_framework.ModuleManager")
            local LogicKillCounter = ModuleManager.GetModule(ModuleManager.CommonModuleConfig.LogicKillCounter)
            local curEquipedKillCounter = LogicKillCounter:GetEquipedKillCounterId(6114302174, currentEquipAvatarID)
            if not curEquipedKillCounter or curEquipedKillCounter == 0 then
                curEquipedKillCounter = LogicKillCounter:GetBaseKillCounterIdByWeaponId(DefineID)
            end
            local kills = _G.getKills(DefineID)
            MainKillCounter:SetKillCounterItemShowWithNum(curEquipedKillCounter, kills, currentEquipAvatarID)
            if MainKillCounter.KillCounterItem and MainKillCounter.KillCounterItem.SetVisibility then
                local ESlateVisibility = import("ESlateVisibility")
                MainKillCounter.KillCounterItem:SetVisibility(ESlateVisibility.Collapsed)
                MainKillCounter.KillCounterItem:SetVisibility(ESlateVisibility.SelfHitTestInvisible)
            end
        end
    end)
end

function _G.FileWatcher()
    if not _G.isFileWatcherActive then return end
    pcall(function()
        if not _G.ActiveKillCounterPath then _G.ActiveKillCounterPath = _G.GetKillCounterPath() end
        local file = io.open(_G.ActiveKillCounterPath, 'r')
        if not file then return end
        local currentContent = file:read('*a') or ""
        file:close()
        currentContent = currentContent:gsub('\239\187\191', ''):gsub('^%s+', ''):gsub('%s+$', '')
        if currentContent == "" or currentContent == _G.lastFileContent then return end
        _G.lastFileContent = currentContent
        local tempTable = {}
        for weaponID, count in currentContent:gmatch('%[(%d+)%]%s*=%s*(%d+)') do
            tempTable[tonumber(weaponID)] = tonumber(count)
        end
        if not next(tempTable) then return end
        _G.killCountInfo = tempTable
        _G.ForceUpdateKillCounterUI()
    end)
end

pcall(function()
    local MyDamageNumMainUI = require("GameLua.Mod.Library.Client.UI.DamageNumMainUI")
    if MyDamageNumMainUI then
        local UWidgetLayoutLibrary = import("WidgetLayoutLibrary")
        local GameplayData = require("GameLua.GameCore.Data.GameplayData")
        MyDamageNumMainUI.__inner_impl.ShowDamage = function(self, Damage, X, Y, Z, uFSlateColor, nFontSize)
            if not self.FlyNumItemPool or Damage == 0 then return end
            local Item = self.FlyNumItemPool:GetOneItem()
            self.UIRoot.CanvasPanel_28:AddChild(Item)
            local damageInfo = { item = Item, worldPosition = FVector(X, Y, Z), updateHandle = nil }
            local uPlayerController = GameplayData.GetPlayerController()
            local function UpdateScreenPosition()
                if slua.isValid(damageInfo.item) then
                    local ScreenPos = UWidgetLayoutLibrary.ProjectWorldLocationToWidgetPositionReturnValue(uPlayerController, damageInfo.worldPosition)
                    if ScreenPos then damageInfo.item:SetRenderTranslation(ScreenPos) end
                end
            end
            UpdateScreenPosition()
            damageInfo.updateHandle = self:AddGameTimer(0.016, true, function()
                if slua.isValid(damageInfo.item) then UpdateScreenPosition()
                else if damageInfo.updateHandle then self:RemoveGameTimer(damageInfo.updateHandle) end end
            end)
            Item.DamageText:SetText(tostring(Damage))
            if slua.isValid(uFSlateColor) then Item.DamageText:SetColorAndOpacity(uFSlateColor)
            else Item.DamageText:SetColorAndOpacity(FSlateColor(FLinearColor(1, 1, 1, 1))) end
            local Font = Item.DamageText.Font
            Font.Size = (nFontSize and type(nFontSize) == "number") and nFontSize or 18
            Item.DamageText:SetFont(Font)
            local animTime = 5.0
            if _G.bFadeAnim then
                Item:PlayAnimation(Item.Fadein, 0, 1, 0, 1)
                animTime = Item.Fadein:GetEndTime()
            end
            self:AddGameTimer(animTime, false, function()
                if slua.isValid(Item) then
                    if damageInfo.updateHandle then self:RemoveGameTimer(damageInfo.updateHandle) end
                    self.FlyNumItemPool:FreeOneItem(Item)
                end
            end)
        end
    end
end)

local function installOutfitHooks()
    if _G.__SKIN_OUTFIT_HOOKS then return end
    _G.__SKIN_OUTFIT_HOOKS = true
    pcall(function()
        local lobby_main = require("client.slua.umg.lobby.Main.Lobby_Main_UIBP")
        if lobby_main and lobby_main.__inner_impl then
            for _, methodName in ipairs({ "OnShow", "OnOpen", "Construct", "RegistEvents" }) do
                local orig = lobby_main.__inner_impl[methodName]
                if orig then
                    lobby_main.__inner_impl[methodName] = function(self, ...)
                        local ret = orig(self, ...)
                        pcall(function()
                            _G.__PLAYABLE_GAME_ACTIVE = false
                            _G.__outfitRetryChain = nil
                            _G.ReadConfigFile(true)
                            _G.ApplyLobbyTheme()
                            _G.GameAvatarHandlerplayers()
                        end)
                        return ret
                    end
                end
            end
        end
    end)
    pcall(function()
        local function allowOutfitRes(resID)
            if _G.isAllowedOutfitRes and _G.isAllowedOutfitRes(resID) then return true end
            local suit = tonumber(_G.SuitSkin) or 0
            if suit > 0 and tonumber(resID) == suit then return true end
            if _G.isInjectedSkinRes and _G.isInjectedSkinRes(resID) then return true end
            return false
        end
        local function hookCheck(comp)
            if not comp or not comp.CheckItemValid then return end
            local o = comp.CheckItemValid
            comp.CheckItemValid = function(self, resID)
                if allowOutfitRes(resID) then return true end
                return o(self, resID)
            end
        end
        hookCheck(require("GameLua.Mod.Library.GamePlay.Avatar.Component.CharacterAvatarComponent"))
        pcall(function()
            hookCheck(require("GameLua.Mod.Library.GamePlay.Avatar.Component.CharacterAvatarComponent2"))
        end)
    end)
    pcall(function()
        local function hookMeshOutfitApply(CAC)
            if not CAC or not CAC.OnAvatarAllMeshLoadedLua then return end
            local orig = CAC.OnAvatarAllMeshLoadedLua
            CAC.OnAvatarAllMeshLoadedLua = function(self)
                orig(self)
                pcall(function()
                    if self.IsLobbyActor and self:IsLobbyActor() then return end
                    local isSelf = false
                    pcall(function()
                        if self.IsSelf and self:IsSelf() then isSelf = true end
                    end)
                    if not isSelf or isInLobby() or not isInPlayableGame() then return end
                    local suitSkin = resolveSuitSkinForAvatar()
                    if suitSkin <= 0 or not _G.applySuitToMatchComp then return end
                    local ok = _G.applySuitToMatchComp(self, suitSkin)
                    if ok then
                        _G.__lastAppliedMatchOutfit = suitSkin
                        _G.__matchOutfitOk = true
                        skinProbeLog('mesh outfit apply shirt=' .. suitSkin .. ' ok=true')
                    end
                end)
            end
        end
        hookMeshOutfitApply(require("GameLua.Mod.Library.GamePlay.Avatar.Component.CharacterAvatarComponent"))
        pcall(function()
            hookMeshOutfitApply(require("GameLua.Mod.Library.GamePlay.Avatar.Component.CharacterAvatarComponent2"))
        end)
    end)
end

local function installWeaponEventHooks()
    if _G.__SKIN_WEAPON_HOOKS then return end
    _G.__SKIN_WEAPON_HOOKS = true
    _G.WeaponEvents = _G.WeaponEvents or { onWeaponChanged = function() end }
    _G.WeaponEvents.onWeaponChanged = function(weaponId)
        pcall(function()
            if not _G.WeaponSkinIndex or not next(_G.WeaponSkinIndex) then
                _G.ReadConfigFile()
            end
            local uCharacter = getSkinPlayerCharacter()
            if uCharacter then
                local currweapon = uCharacter:GetCurrentWeapon()
                if currweapon and slua.isValid(currweapon) then
                    _G.ProcessOneWeapon(currweapon, true)
                end
            end
        end)
        pcall(function()
            local uCharacter = getSkinPlayerCharacter()
            if not uCharacter or not _G.OurkillCountSystem then return end
            local currweapon = uCharacter:GetCurrentWeapon()
            if not currweapon then return end
            local DefineID = currweapon:GetItemDefineID().TypeSpecificID
            local SkinID = slua.IndexReference(currweapon.synData:Get(7), "defineID").TypeSpecificID
            _G.OurkillCountSystem:UpdateMainKillCounterUI(true, DefineID, SkinID)
        end)
    end
    pcall(function()
        local SlotBase = require("GameLua.Mod.BaseMod.Client.MainControlUI.SwitchWeaponSlotMode2")
        if not SlotBase or not SlotBase.__inner_impl then return end
        for _, methodName in ipairs({ "SwitchWeapon", "OnTouchWeaponSlot", "EquipWeaponBySlot" }) do
            local orig = SlotBase.__inner_impl[methodName]
            if orig then
                SlotBase.__inner_impl[methodName] = function(self, ...)
                    local ret = orig(self, ...)
                    pcall(function()
                        local uCharacter = getSkinPlayerCharacter()
                        if uCharacter then
                            _G.ProcessOneWeapon(uCharacter:GetCurrentWeapon(), true)
                        end
                    end)
                    return ret
                end
            end
        end
    end)
end

local function installExtendedSkinHooks()
    if _G.__SKIN_EXTENDED_HOOKS then return end
    _G.__SKIN_EXTENDED_HOOKS = true
    installWeaponEventHooks()
    installOutfitHooks()

    pcall(function()
local SKillInfo = require("GameLua.Mod.BaseMod.Client.KillInfoTips.KillInfo")
local ECharacterHealthStatus = import("ECharacterHealthStatus")
local SKillInfoModuleManager = require("client.module_framework.ModuleManager")
local O_FileItem = SKillInfo.__inner_impl.FileItem

SKillInfo.__inner_impl.FileItem = function(self, DamageRecordData)
    if not self or not DamageRecordData then return O_FileItem(self, DamageRecordData) end
    local LogicKillCounter = SKillInfoModuleManager.GetModule(SKillInfoModuleManager.CommonModuleConfig.LogicKillCounter)
    if not LogicKillCounter then return O_FileItem(self, DamageRecordData) end
    local uCharacter = slua_GameFrontendHUD and slua_GameFrontendHUD:GetPlayerController() and slua_GameFrontendHUD:GetPlayerController():GetPlayerCharacterSafety()
    if not uCharacter or not slua.isValid(uCharacter) then return O_FileItem(self, DamageRecordData) end
    local SelfName = uCharacter:GetPlayerNameSafety()
    if DamageRecordData.Causer == SelfName then
        local currWeapon = uCharacter:GetCurrentWeapon()
        if currWeapon and slua.isValid(currWeapon) then
            local DefineID = currWeapon:GetItemDefineID() and currWeapon:GetItemDefineID().TypeSpecificID or 0
            if DefineID ~= 0 then
                local ExpandData = slua.LuaArchiverDecode(LuaStateWrapper, DamageRecordData.ExpandDataContent) or {}
                local SupportKillCounter = LogicKillCounter:GetBaseKillCounterIdByWeaponId(DefineID)
                if SupportKillCounter and DamageRecordData.ResultHealthStatus == ECharacterHealthStatus.FinishedLastBreath then
                    ExpandData.KillCounterItemId = DefineID
                    ExpandData.KillCounterNum = (ExpandData.KillCounterNum or 0) + 1
                    _G.addKill(DefineID, 1)
                end
                local synData = currWeapon.synData
                if synData and slua.isValid(synData) then
                    local weaponDefineID = slua.IndexReference(synData:Get(7), "defineID")
                    if weaponDefineID and slua.isValid(weaponDefineID) then
                        DamageRecordData.CauserWeaponAvatarID = weaponDefineID.TypeSpecificID
                    end
                end
                DamageRecordData.ExpandDataContent = slua.LuaArchiverEncode(LuaStateWrapper, ExpandData)
            end
        end
    end
    O_FileItem(self, DamageRecordData)
end

local MyMainKillCounter = require("GameLua.Mod.BaseMod.Client.KillCounter.MainKillCounter")
local MyKillCountSubSystem = require("GameLua.Mod.BaseMod.Client.KillCounter.KillCounterUISubsystem")
_G.OurkillCountSystem = MyKillCountSubSystem.__inner_impl

MyMainKillCounter.__inner_impl.OnRefreshUI = function(self, _, _, UID)
    pcall(function()
        local ModuleManager = require("client.module_framework.ModuleManager")
        local LogicKillCounter = ModuleManager.GetModule(ModuleManager.CommonModuleConfig.LogicKillCounter)
        local uCharacter = slua_GameFrontendHUD:GetPlayerController():GetPlayerCharacterSafety()
        if not uCharacter then return end
        local currweapon = uCharacter:GetCurrentWeapon()
        if currweapon then
            local DefineID = currweapon:GetItemDefineID().TypeSpecificID
            local currentEquipAvatarID = slua.IndexReference(currweapon.synData:Get(7), "defineID").TypeSpecificID
            local curEquipedKillCounter = LogicKillCounter:GetEquipedKillCounterId(6114302174, currentEquipAvatarID)
            self.KillCounterItem:SetKillCounterItemShowWithNum(curEquipedKillCounter, _G.getKills(DefineID), currentEquipAvatarID)
        end
    end)
end

MyKillCountSubSystem.__inner_impl.CheckSupportKCUI = function(self) return true end

local o_CheckNeedMainKillCounterUI = MyKillCountSubSystem.__inner_impl.CheckNeedMainKillCounterUI
MyKillCountSubSystem.__inner_impl.CheckNeedMainKillCounterUI = function(self, Weapon, PlayerID)
    pcall(function()
        local uCharacter = slua_GameFrontendHUD:GetPlayerController():GetPlayerCharacterSafety()
        if not uCharacter then return end
        local currweapon = uCharacter:GetCurrentWeapon()
        if currweapon then
            local DefineID = currweapon:GetItemDefineID().TypeSpecificID
            _G.WeaponEvents.onWeaponChanged(DefineID)
            self:UpdateMainKillCounterUI(true, DefineID, slua.IndexReference(currweapon.synData:Get(7), "defineID").TypeSpecificID)
        end
    end)
end

local o_UpdateMainKillCounterUI = MyKillCountSubSystem.__inner_impl.UpdateMainKillCounterUI
MyKillCountSubSystem.__inner_impl.UpdateMainKillCounterUI = function(self, bShow, WeaponID, AvatarID)
    pcall(function()
        o_UpdateMainKillCounterUI(self, bShow, WeaponID, AvatarID)
        local UIManager = require("client.slua_ui_framework.manager")
        local MainKillCounter = UIManager.GetUI(UIManager.UI_Config_InGame.MainKillCounter)
        local uCharacter = slua_GameFrontendHUD:GetPlayerController():GetPlayerCharacterSafety()
        if not uCharacter then return end
        local currweapon = uCharacter:GetCurrentWeapon()
        if not bShow and MainKillCounter then
            UIManager.CloseUI(UIManager.UI_Config_InGame.MainKillCounter)
        elseif bShow and currweapon then
            local DefineID = currweapon:GetItemDefineID().TypeSpecificID
            local currentEquipAvatarID = slua.IndexReference(currweapon.synData:Get(7), "defineID").TypeSpecificID
            local ModuleManager = require("client.module_framework.ModuleManager")
            local LogicKillCounter = ModuleManager.GetModule(ModuleManager.CommonModuleConfig.LogicKillCounter)
            local SupportKillCounter = LogicKillCounter:GetBaseKillCounterIdByWeaponId(DefineID)
            if SupportKillCounter == nil and MainKillCounter then
                UIManager.CloseUI(UIManager.UI_Config_InGame.MainKillCounter)
            elseif DefineID == currentEquipAvatarID and MainKillCounter then
                UIManager.CloseUI(UIManager.UI_Config_InGame.MainKillCounter)
            else
                local curEquipedKillCounter = LogicKillCounter:GetEquipedKillCounterId(6114302174, currentEquipAvatarID)
                if not MainKillCounter then
                    UIManager.ShowUI(UIManager.UI_Config_InGame.MainKillCounter, DefineID, currentEquipAvatarID)
                    MainKillCounter = UIManager.GetUI(UIManager.UI_Config_InGame.MainKillCounter)
                    if MainKillCounter then
                        MainKillCounter:SetKillCounterItemShowWithNum(curEquipedKillCounter, _G.getKills(DefineID), currentEquipAvatarID)
                    end
                else
                    MainKillCounter:UpdateWeaponID(DefineID, currentEquipAvatarID)
                    MainKillCounter:SetKillCounterItemShowWithNum(curEquipedKillCounter, _G.getKills(DefineID), currentEquipAvatarID)
                end
            end
        end
    end)
end
    end)
end

pcall(function()
    local IngamePhoneStateUI = require("GameLua.Mod.Library.Client.UI.IngamePhoneStateUI")
    local Lobby_Main_Wifi_UIBP = require("client.slua.umg.lobby.Main.Lobby_Main_Wifi_UIBP")
    local o_UpdateQuality = Lobby_Main_Wifi_UIBP.__inner_impl.UpdateQuality
    Lobby_Main_Wifi_UIBP.__inner_impl.UpdateQuality = function(self)
        self.UIRoot.WidgetSwitcher_Quality:SetActiveWidgetIndex(0)
        self.UIRoot.TextBlock_High:SetText("Anubis")
        self.UIRoot.TextBlock_High:SetColorAndOpacity(FSlateColor(FLinearColor(1, 1, 1, 1)))
    end
    local o_UpdateArtQualityUI = IngamePhoneStateUI.__inner_impl.UpdateArtQualityUI
    IngamePhoneStateUI.__inner_impl.UpdateArtQualityUI = function(self, _, _)
        self.UIRoot.TextBlock_quality:SetText("Anubis")
        self.UIRoot.TextBlock_quality:SetColorAndOpacity(FSlateColor(FLinearColor(0, 1, 0, 1)))
    end
end)

local function download_item(id)
    local PufferManager = require('client.slua.logic.download.puffer.puffer_manager')
    local PufferConst = require('client.slua.logic.download.puffer_const')
    if not PufferManager or not PufferConst then return end
    local state = PufferManager.GetState(PufferConst.ENUM_DownloadType.ODPAK, {id})
    if state ~= PufferConst.ENUM_DownloadState.Done then
        PufferManager.Download(PufferConst.ENUM_DownloadType.ODPAK, {id})
    end
end
_G.download_item = download_item

_G.IsPtrValid = function(ptr)
    if ptr == nil then return false end
    return slua.isValid(ptr)
end

function _G.apply_attachment(CurWeapon, avatarid)
    if not CurWeapon or not avatarid or avatarid == 0 then return end
    pcall(loadSkinAttachmentMaps)
    avatarid = getWeaponAppliedSkinId(CurWeapon, avatarid)
    pcall(function() applyBaseOverridesToAttachmentMap(avatarid) end)
    if not hasAttachmentOverrideMap(avatarid) then
        pcall(function() _G.InitParts(get_group_id(avatarid), avatarid) end)
    end
    local array = CurWeapon.synData
    if not array then return end
    local needsMeshRefresh = false
    for AttachIdx = 0, 4 do
        local isrefresh = false
        local Data = array:Get(AttachIdx)
        if not Data then break end
        local itemid = slua.IndexReference(Data, "defineID").TypeSpecificID or 0
        if itemid < 10000000 then
            local newId, isrefresh = applyAttachmentSkinForSlot(AttachIdx, itemid, avatarid)
            if isrefresh and newId then
                Data.defineID.TypeSpecificID = newId
                array:Set(AttachIdx, Data)
                needsMeshRefresh = true
            end
        end
    end
    if needsMeshRefresh then
        pcall(function() CurWeapon:DelayHandleAvatarMeshChanged() end)
    end
end

function _G.ResetMatchState()
    _G.MatchEndMessageShown = false
    _G.LastKillTime = {}
    for k in pairs(_G.AlreadyChangedSet) do _G.AlreadyChangedSet[k] = nil end
    for k in pairs(_G.DeadBoxSkins) do _G.DeadBoxSkins[k] = nil end
end

local function ShowMatchEndMessage(message)
    pcall(function()
        local CommonMsgBoxMgr = require("client.slua.logic.common.logic_common_msg_box")
        if CommonMsgBoxMgr then
            CommonMsgBoxMgr.Show(1, "Anubis", message, function() end)
        end
    end)
end

local function GetRandomEndMessage(rank, kills)
    local messages = {
        "Congratulations! You fought well!", "Good game! See you on the battlefield!",
        "Well played soldier!", "Victory is in your blood!", "Another battle, another victory!",
        "You are a true champion!", "The battlefield honors you!", "Great performance today!",
        "Your skills are impressive!", "Keep up the good work!", "You made your team proud!",
        "Legendary performance!", "You are unstoppable!", "Fear the reaper! You are death itself!",
        "Anubis sends their regards!"
    }
    if rank == 1 then
        table.insert(messages, "WINNER WINNER CHICKEN DINNER!")
        table.insert(messages, "You are the last one standing! AMAZING!")
        table.insert(messages, "CHAMPION! You dominated the battlefield!")
    elseif rank <= 5 then
        table.insert(messages, "Top 5 finish! Excellent work!")
        table.insert(messages, "Almost there! Next time you will win!")
    end
    if kills >= 15 then table.insert(messages, "HATTRICK! " .. kills .. " kills! You are a beast!")
    elseif kills >= 10 then table.insert(messages, "Double digits! " .. kills .. " kills! Impressive!")
    elseif kills >= 5 then table.insert(messages, "Solid " .. kills .. " kills! Well done!")
    end
    return messages[math.random(#messages)]
end

local function OnMatchEnd()
    if _G.MatchEndMessageShown then return end
    pcall(function()
        local GameState = slua_GameFrontendHUD and slua_GameFrontendHUD:GetGameState()
        if not GameState then return end
        local PlayerController = slua_GameFrontendHUD:GetPlayerController()
        if not PlayerController then return end
        local MyKills = 0
        if _G.killCountInfo then
            for weapon, kills in pairs(_G.killCountInfo) do MyKills = MyKills + kills end
        end
        local MyRank = 99
        local TeamRank = GameState:GetTeamRank()
        if TeamRank and TeamRank > 0 then MyRank = TeamRank end
        local message = GetRandomEndMessage(MyRank, MyKills)
        message = message .. "\n\nRank: " .. MyRank .. "\nKills: " .. MyKills .. "\n\nAnubis"
        ShowMatchEndMessage(message)
        _G.MatchEndMessageShown = true
        _G.ResetMatchState()
    end)
end

local function CheckGameEndLoop()
    pcall(function()
        local GameState = slua_GameFrontendHUD and slua_GameFrontendHUD:GetGameState()
        if GameState and GameState.bIsMatchEnd and not _G.MatchEndMessageShown then
            OnMatchEnd()
        end
    end)
end

-- ============================================================
-- [NEW] ProcessOneWeapon
-- Ek weapon ke liye:
--   1. Skin apply karo (config se)
--   2. Agar skin lagi hai (SkinAttachmentMap mein entry hai)
--      to FORCE karo ki attachment bhi sahi hai
--   3. Attachment wrong hai to fix karo ??? cache ki parwah nahi
-- ============================================================
local function weaponAttachmentsNeedFix(weapon, skinId)
    if not _G.__attachMapsLoaded then pcall(loadSkinAttachmentMaps) end
    local array = weapon.synData
    if not array or not skinId then return false end
    for AttachIdx = 0, 4 do
        local Data = array:Get(AttachIdx)
        if not Data then break end
        local currentId = slua.IndexReference(Data, "defineID").TypeSpecificID or 0
        if currentId > 0 and currentId < 10000000 then
            local correctId, changed = applyAttachmentSkinForSlot(AttachIdx, currentId, skinId)
            if changed and correctId and correctId ~= currentId then
                return true
            end
        end
    end
    return false
end

function _G.ProcessOneWeapon(weapon, force)
    if not weapon or not slua.isValid(weapon) then return end

    local weaponid = weapon:GetItemDefineID().TypeSpecificID
    if not weaponid or weaponid == 0 then return end

    local baseId = resolveBaseWeaponId(weaponid)
    local targetSkinID = _G.WeaponSkinIndex and _G.WeaponSkinIndex[baseId]
    if not targetSkinID or targetSkinID == 0 then return end

    local synData = weapon.synData
    local slot7 = synData and synData:Get(7)
    local currentSkin = slot7 and slua.IndexReference(slot7, "defineID").TypeSpecificID
    local attachSkinId = getWeaponAppliedSkinId(weapon, targetSkinID)
    local attachFix = weaponAttachmentsNeedFix(weapon, attachSkinId)

    if not force and currentSkin == targetSkinID and not attachFix then
        return
    end

    _G.apply_weapon_skin(weapon, baseId)
end

local function ProcessOneWeapon(weapon, force)
    _G.ProcessOneWeapon(weapon, force)
end

-- Rare fallback only — weapon switch hooks do the real work
local _lastWeaponPoll = 0
local function GameAvatarHandlerweapons()
    local now = os.clock()
    if now - _lastWeaponPoll < 2.5 then return end
    _lastWeaponPoll = now
    pcall(function()
        if checkSkinReloadStamp() then _lastWeaponPoll = 0 end
        local uCharacter = getMatchPlayerCharacter() or getSkinPlayerCharacter()
        if not uCharacter then return end
        local currweapon = uCharacter:GetCurrentWeapon()
        if currweapon and slua.isValid(currweapon) then
            _G.ProcessOneWeapon(currweapon, false)
        end
    end)
end

local function GameAvatarHandlerkillcounter()
    pcall(_G.ForceUpdateKillCounterUI)
end

local _skinUnifiedTickN = 0

local function skinModUnifiedTick()
    _skinUnifiedTickN = _skinUnifiedTickN + 1
    local n = _skinUnifiedTickN
    pcall(function()
        if checkSkinReloadStamp() then
            _G.__matchOutfitOk = nil
            _G.__lastAppliedMatchOutfit = nil
            _G.__lastGearAvatarTick = nil
        end
    end)
    if isInPlayableGame() then
        pcall(GameAvatarHandlerweapons)
        if n % 2 == 0 then pcall(_G.GameAvatarHandlervehicles) end
        if n % 3 == 0 then pcall(GameAvatarHandlerkillcounter) end
        if not _G.__matchOutfitOk and n % 2 == 0 then
            pcall(function() _G.applyMatchOutfitNow(true) end)
        elseif n % 6 == 0 then
            pcall(_G.applyMatchOutfitIfNeeded)
        end
        if n % 4 == 0 then pcall(_G.GameAvatarHandlerplayers) end
        if n % 5 == 0 then pcall(_G.GameAvatarHandlerDeadBox) end
        if n % 10 == 0 then pcall(_G.FileWatcher) end
        if n % 2 == 0 then pcall(CheckGameEndLoop) end
    else
        if n % 2 == 0 then
            pcall(function()
                if not _G.__lastOutfitConfigRead or (os.clock() - _G.__lastOutfitConfigRead) > 45 then
                    _G.ReadConfigFile()
                    _G.__lastOutfitConfigRead = os.clock()
                end
                if _G.__needsLobbyOutfitRefresh and _G.refreshLobbyOutfitVisual then
                    _G.refreshLobbyOutfitVisual(true)
                end
                _G.GameAvatarHandlerplayers()
            end)
        end
        if n % 3 == 0 then
            pcall(_G.CheckLobbyThemeChanges)
            pcall(_G.ApplyLobbyTheme)
        end
        if n % 4 == 0 then pcall(_G.HandlePetLogic) end
        if n % 10 == 0 then pcall(_G.scheduleLobbyConfigFlush) end
    end
end

function _G.startSrcOutfitTimers()
    if _G.__SRC_HUB_TIMERS_STARTED then return end
    local ok, ticker = pcall(require, 'common.time_ticker')
    if ok and ticker then _G.Mytimer_ticker = ticker end
    if not _G.Mytimer_ticker then return end

    if not _G.__SKIN_BOOTSTRAPPED then
        pcall(_G.loadKillCountFromFile)
        _G.isFileWatcherActive = true
        pcall(InitItemUpgradeSystem)
        pcall(installExtendedSkinHooks)
        pcall(loadBuiltinAttachmentMaps)
        _G.__SKIN_BOOTSTRAPPED = true
    end
    pcall(_G.ReadConfigFile, true)

    _G.__SRC_HUB_TIMERS_STARTED = true
    skinProbeLog('startSrcOutfitTimers: unified 3s schedule')
    pcall(function()
        _G.Mytimer_ticker.AddTimerLoop(3, skinModUnifiedTick, -1, 1)
        _G.Mytimer_ticker.AddTimerOnce(0.8, function()
            pcall(_G.ApplyLobbyTheme)
            pcall(_G.GameAvatarHandlerplayers)
            pcall(function() _G.applyMatchOutfitNow(true) end)
            pcall(_G.HandlePetLogic)
            local uCharacter = getMatchPlayerCharacter() or getSkinPlayerCharacter()
            if uCharacter then
                pcall(function() _G.ProcessOneWeapon(uCharacter:GetCurrentWeapon(), true) end)
            end
        end)
    end)
end
_G.startSrcIngameTimers = _G.startSrcOutfitTimers
_G.ensureSkinTimers = _G.startSrcOutfitTimers

pcall(function()
    local ok, ticker = pcall(require, 'common.time_ticker')
    if ok and ticker then _G.Mytimer_ticker = ticker end
end)

_G.__SKIN_PATCHED = true
pcall(loadBuiltinAttachmentMaps)

if _G.Mytimer_ticker then
    pcall(function()
        _G.Mytimer_ticker.AddTimerOnce(1, function()
            if _G.startSrcOutfitTimers then _G.startSrcOutfitTimers() end
        end)
    end)
else
    pcall(function()
        if _G.startSrcOutfitTimers then _G.startSrcOutfitTimers() end
    end)
end
