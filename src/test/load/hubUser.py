class HubUser:
    def __init__(self):
        pass

    def name(self):
        raise NotImplementedError("HubUser.name must be implemented")

    def start_channel(self, payload, tasks):
        pass

    def channel_post_url(self, channel):
        return "/channel/" + channel

    def has_webhook(self):
        return True

    def has_websocket(self):
        return True

    def time_path(self, unit="second"):
        return "/time/" + unit + "?stable=false"

    def start_webhook(self, config):
        pass

    def skip_verify_ordered(self):
        return False
