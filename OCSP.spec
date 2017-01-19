%global         _prefix /usr

Name:           OCDP_Stream
Version:        2.0
Release:        1_beta_k
Summary:        OCSP from asiainfo.com

Group:          Applications/Productivity
License:        GPL
URL:            https://github.com/OCSP/OCSP_mainline
Prefix:         %{_prefix}

%description
OCSP from ASIAINFO

%prep

%install
rm -rf %{buildroot}
%{__install} -d %{buildroot}%{_prefix}
tar -xzf %{_sourcedir}/OCDP_Stream_2.0.1_beta_k.tar.gz -C %{buildroot}%{_prefix}
mkdir -p %{buildroot}%{_prefix}/ocsp
mv %{buildroot}%{_prefix}/OCDP_Stream/* %{buildroot}%{_prefix}/ocsp

%post

%preun
rpm_name="OCDP_Stream-2.0-1_beta_k.x86_64"
rpm_path=`rpm -ql  ${rpm_name} | head -n 1 | awk -F/ 'NF-=1' OFS=/`
# stop the service
sh ${rpm_path}/ocsp/bin/stream stop
sh ${rpm_path}/ocsp/bin/fountain stop

%files
%dir %{_prefix}/ocsp
%{_prefix}/ocsp/bin
%{_prefix}/ocsp/conf
%{_prefix}/ocsp/lib
%{_prefix}/ocsp/logs
%{_prefix}/ocsp/web
%defattr (755,root,root)
%doc

%changelog
