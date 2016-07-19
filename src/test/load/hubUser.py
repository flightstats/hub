class HubUser:
    def __init__(self):
        pass

    def name(self):
        raise NotImplementedError("HubUser.name must be implemented")

    def channel_payload(self, payload):
        pass
