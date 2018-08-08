import ipaddress


def get_client_address(request):
    unicode_address = u'{}'.format(request.remote_addr)
    address = ipaddress.ip_address(unicode_address)
    if isinstance(address, ipaddress.IPv6Address) and address.ipv4_mapped:
        return str(address.ipv4_mapped)
    else:
        return str(address)
