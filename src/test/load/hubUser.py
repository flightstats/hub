class HubUser:
    def __init__(self):
        pass

    def name(self):
        raise NotImplementedError("HubUser.name must be implemented")

    def channel_payload(self, payload):
        pass

    def channel_post_url(self, channel):
        return "/channel/" + channel

    def has_webhook(self):
        return True

    def has_websocket(self):
        return True

    def time_path(self, unit="second"):
        return "/time/" + unit + "?stable=false"
