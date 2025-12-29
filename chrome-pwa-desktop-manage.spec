Name:           chrome-pwa-desktop-manage
Version:        1.0.6
Release:        1%{?dist}
Summary:        Manage Chrome Progressive Web Apps desktop shortcuts

License:        MIT
URL:            https://github.com/lixiujunj2ee/chrome-pwa-desktop-manage

# 关键修复点：使用 GitHub 自动生成的源码包
Source0:        %{url}/archive/refs/tags/v%{version}.tar.gz

BuildArch:      noarch
Requires:       python3

%description
A user-friendly tool to manage Chrome PWA desktop shortcuts on Linux.
Provides GUI management and ensures single-instance opening of PWAs.

%prep
# GitHub archive 解压后目录名是：项目名-版本号
%autosetup -n %{name}-%{version}

%install
rm -rf %{buildroot}

# ===== 可执行命令 =====
install -Dm755 pwa %{buildroot}%{_bindir}/pwa
install -Dm755 start_app.sh %{buildroot}%{_bindir}/chrome-pwa-desktop-manage

# ===== Python 源码 =====
install -d %{buildroot}%{_datadir}/%{name}
install -Dm644 main.py %{buildroot}%{_datadir}/%{name}/main.py
cp -a pwa/*.py *.py %{buildroot}%{_datadir}/%{name}/ 2>/dev/null || :

# ===== Desktop 文件 =====
install -Dm644 com.github.lixiujunj2ee.ChromePWADesktopManage.desktop \
  %{buildroot}%{_datadir}/applications/com.github.lixiujunj2ee.ChromePWADesktopManage.desktop

# ===== 图标 =====
install -Dm644 icons/chrome-pwa-desktop-manage.png \
  %{buildroot}%{_datadir}/icons/hicolor/128x128/apps/com.github.lixiujunj2ee.ChromePWADesktopManage.png

# ===== AppStream =====
install -Dm644 com.github.lixiujunj2ee.ChromePWADesktopManage.metainfo.xml \
  %{buildroot}%{_metainfodir}/com.github.lixiujunj2ee.ChromePWADesktopManage.metainfo.xml

%post
update-desktop-database &>/dev/null || :
gtk-update-icon-cache %{_datadir}/icons/hicolor &>/dev/null || :

%postun
update-desktop-database &>/dev/null || :
gtk-update-icon-cache %{_datadir}/icons/hicolor &>/dev/null || :

%files
%{_bindir}/pwa
%{_bindir}/chrome-pwa-desktop-manage
%dir %{_datadir}/%{name}
%{_datadir}/%{name}/
%{_datadir}/applications/com.github.lixiujunj2ee.ChromePWADesktopManage.desktop
%{_datadir}/icons/hicolor/128x128/apps/com.github.lixiujunj2ee.ChromePWADesktopManage.png
%{_metainfodir}/com.github.lixiujunj2ee.ChromePWADesktopManage.metainfo.xml

%changelog
* Mon Dec 29 2025 lixiujunj2ee - 1.0.6-1
- Fix Copr build: use GitHub archive as Source0
- Correct autosetup usage
- Stable Copr SRPM build
