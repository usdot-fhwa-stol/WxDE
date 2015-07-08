from os import environ, makedirs
from os.path import exists, join

System_root_dir = "/home/zhengg/vdt/data"

Log_dir  = join(System_root_dir, "log")
Tmp_dir  = join(System_root_dir, "tmp")
Static_dir = join(System_root_dir, "static")
Raw_dir = join(System_root_dir, "raw")

Static_site_list_dir = join(Static_dir, "site_list")
NA_site_list_file = join(Static_site_list_dir, "northamerica_sites.asc")

Static_cdl_dir = join(Static_dir, "cdl")
Metar2nc_cdl_file = join(Static_cdl_dir, "metar.cdl")

Raw_metar_dir = join(Raw_dir, "metar")
Raw_metar_ascii_dir = join(Raw_metar_dir, "ascii")
Raw_metar_netcdf_dir = join(Raw_metar_dir, "netcdf")
