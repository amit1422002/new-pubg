_G.__GUEST_LOGIN_PATCHED = false

function _G.ForceEnableGuestLogin()
    pcall(function()
        local ModuleManager = require("client.module_framework.ModuleManager")
        if not ModuleManager then return end

        local login_module = ModuleManager.GetModule(ModuleManager.LobbyModuleConfig.login_module)
        if not login_module then return end

        local oldIsAvailable = login_module.IsAvailableChannel
        login_module.IsAvailableChannel = function(self, channel)
            if _G.ShareSource then
                if channel == _G.ShareSource.Guest or channel == _G.ShareSource.BgBg then
                    return true
                end
            end
            if ShareSource then
                if channel == ShareSource.Guest or channel == ShareSource.BgBg then
                    return true
                end
            end
            if oldIsAvailable then
                return oldIsAvailable(self, channel)
            end
            return false
        end

        login_module.CanShowMoreLoginChannel = function()
            return true
        end

        login_module.LoginTypeList = nil
        login_module.LoginTypeTable = nil

        local oldGetLoginTypeList = login_module.GetLoginTypeList
        login_module.GetLoginTypeList = function(self)
            local list = oldGetLoginTypeList(self)

            if list then
                local gGuest = (_G.ShareSource and _G.ShareSource.Guest) or (ShareSource and ShareSource.Guest)
                local gBgBg = (_G.ShareSource and _G.ShareSource.BgBg) or (ShareSource and ShareSource.BgBg)

                if gGuest or gBgBg then
                    local hasBgBg = false
                    local hasGuest = false
                    for _, ch in pairs(list) do
                        if gBgBg and ch == gBgBg then hasBgBg = true end
                        if gGuest and ch == gGuest then hasGuest = true end
                    end

                    if not hasBgBg and gBgBg then
                        table.insert(list, gBgBg)
                    end
                    if not hasGuest and gGuest then
                        table.insert(list, gGuest)
                    end
                end
            end

            return list
        end

        local loginUI = login_module:GetLoginUI()
        if loginUI then
            if loginUI.RefreshUI then
                loginUI:RefreshUI()
            end
            if loginUI.ShowLoginButtons then
                loginUI:ShowLoginButtons()
            end
        end

        _G.__GUEST_LOGIN_PATCHED = true
    end)
end

pcall(_G.ForceEnableGuestLogin)
