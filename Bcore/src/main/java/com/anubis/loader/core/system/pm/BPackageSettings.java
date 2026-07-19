package com.anubis.loader.core.system.pm;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.AtomicFile;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.anubis.loader.core.env.BEnvironment;
import com.anubis.loader.core.system.user.BUserHandle;
import com.anubis.loader.entity.pm.InstallOption;
import com.anubis.loader.utils.CloseUtils;
import com.anubis.loader.utils.FileUtils;

/**
 * Created by Milk on 4/21/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class BPackageSettings implements Parcelable {
    public BPackage pkg;
    public int appId;
    public InstallOption installOption;
    public Map<Integer, BPackageUserState> userState = new HashMap<>();
    static final BPackageUserState DEFAULT_USER_STATE = new BPackageUserState();

    public BPackageSettings() {
    }

    public List<BPackageUserState> getUserState() {
        return new ArrayList<>(userState.values());
    }

    public List<Integer> getUserIds() {
        return new ArrayList<>(userState.keySet());
    }

    public void setInstalled(boolean inst, int userId) {
        modifyUserState(userId).installed = inst;
    }

    public boolean getInstalled(int userId) {
        return readUserState(userId).installed;
    }

    public boolean getStopped(int userId) {
        return readUserState(userId).stopped;
    }

    public void setStopped(boolean stop, int userId) {
        modifyUserState(userId).stopped = stop;
    }

    public boolean getHidden(int userId) {
        return readUserState(userId).hidden;
    }

    public void setHidden(boolean hidden, int userId) {
        modifyUserState(userId).hidden = hidden;
    }

    public void removeUser(int userId) {
        userState.remove(userId);
    }

    public BPackageUserState readUserState(int userId) {
        BPackageUserState state = userState.get(userId);
        if (state == null) {
            state = new BPackageUserState();
        }
        state = new BPackageUserState(state);
        // xp模块所有用户可见、如果开启的话
        if (installOption.isFlag(InstallOption.FLAG_XPOSED) &&
                BXposedManagerService.get().isModuleEnable(pkg.packageName) &&
                BXposedManagerService.get().isXPEnable()) {
            state.installed = true;
        }
        if (userId == BUserHandle.USER_ALL) {
            state.installed = true;
        }
        return state;
    }

    private BPackageUserState modifyUserState(int userId) {
        BPackageUserState state = userState.get(userId);
        if (state == null) {
            state = new BPackageUserState();
            userState.put(userId, state);
        }
        return state;
    }

    public boolean save() {
        synchronized (this) {
            Parcel parcel = Parcel.obtain();
            AtomicFile atomicFile = new AtomicFile(BEnvironment.getPackageConf(pkg.packageName));
            FileOutputStream fileOutputStream = null;
            try {
                writeToParcel(parcel, 0);
                parcel.setDataPosition(0);
                fileOutputStream = atomicFile.startWrite();
                FileUtils.writeParcelToOutput(parcel, fileOutputStream);
                atomicFile.finishWrite(fileOutputStream);
                return true;
            } catch (Throwable e) {
                e.printStackTrace();
                atomicFile.failWrite(fileOutputStream);
                return false;
            } finally {
                parcel.recycle();
                CloseUtils.close(fileOutputStream);
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.pkg, flags);
        dest.writeInt(this.appId);
        dest.writeParcelable(this.installOption, flags);
        dest.writeInt(this.userState.size());
        for (Map.Entry<Integer, BPackageUserState> entry : this.userState.entrySet()) {
            dest.writeValue(entry.getKey());
            dest.writeParcelable(entry.getValue(), flags);
        }
    }

    protected BPackageSettings(Parcel in) {
        this.pkg = in.readParcelable(BPackage.class.getClassLoader());
        this.pkg = canonicalizePkg(this.pkg);
        this.appId = in.readInt();
        this.installOption = in.readParcelable(InstallOption.class.getClassLoader());
        this.installOption = canonicalizeInstallOption(this.installOption);
        int userStateSize = in.readInt();
        this.userState = new HashMap<Integer, BPackageUserState>(userStateSize);
        for (int i = 0; i < userStateSize; i++) {
            Integer key = (Integer) in.readValue(Integer.class.getClassLoader());
            BPackageUserState value = in.readParcelable(BPackageUserState.class.getClassLoader());
            value = canonicalizeUserState(value);
            this.userState.put(key, value);
        }
    }

    /** Rehydrate parcel stubs under legacy {@code com.anubis.loader.*} FQCNs to canonical classes. */
    private static BPackage canonicalizePkg(BPackage pkg) {
        if (pkg == null || pkg.getClass() == BPackage.class) {
            return pkg;
        }
        Parcel p = Parcel.obtain();
        try {
            pkg.writeToParcel(p, 0);
            p.setDataPosition(0);
            return BPackage.CREATOR.createFromParcel(p);
        } finally {
            p.recycle();
        }
    }

    private static InstallOption canonicalizeInstallOption(InstallOption opt) {
        if (opt == null || opt.getClass() == InstallOption.class) {
            return opt;
        }
        Parcel p = Parcel.obtain();
        try {
            opt.writeToParcel(p, 0);
            p.setDataPosition(0);
            return InstallOption.CREATOR.createFromParcel(p);
        } finally {
            p.recycle();
        }
    }

    private static BPackageUserState canonicalizeUserState(BPackageUserState state) {
        if (state == null || state.getClass() == BPackageUserState.class) {
            return state;
        }
        Parcel p = Parcel.obtain();
        try {
            state.writeToParcel(p, 0);
            p.setDataPosition(0);
            return BPackageUserState.CREATOR.createFromParcel(p);
        } finally {
            p.recycle();
        }
    }

    public static final Creator<BPackageSettings> CREATOR = new Creator<BPackageSettings>() {
        @Override
        public BPackageSettings createFromParcel(Parcel source) {
            return new BPackageSettings(source);
        }

        @Override
        public BPackageSettings[] newArray(int size) {
            return new BPackageSettings[size];
        }
    };
}
