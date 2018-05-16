import ipaddress


def get_client_address(request):
    address = ipaddress.ip_address(request.remote_addr)
    if isinstance(address, ipaddress.IPv6Address) and address.ipv4_mapped:
        return str(address.ipv4_mapped)
    else:
        return str(address)
