# ChatAppServer #
# Michael Shachar #
# Ido Ben El #
# Michal Bar Ilan #
# Betzalel Moshkovitz #

import urllib
import urllib2
import webapp2
import json
import ast
from google.appengine.api import urlfetch
from google.appengine.ext import ndb
from time import gmtime, strftime
from google.appengine.api import users
from google.appengine.api import app_identity

####################################################################################
################################                    ################################
################################  Basic ndb Classes   ##############################
################################                    ################################
####################################################################################


# Channel ndb class
class Channel(ndb.Model):
    channel_id = ndb.StringProperty()
    name = ndb.StringProperty()
    icon = ndb.StringProperty()
    is_my = ndb.BooleanProperty()


# Server ndb class
class Server(ndb.Model):
    link = ndb.StringProperty()
    is_connected = ndb.BooleanProperty()


# ChannelToRemove ndb class
class ChannelToRemove(ndb.Model):
    channel_id = ndb.StringProperty()
    link_to_server = ndb.StringProperty()


# Message ndb class
class Message(ndb.Model):
    channel_id = ndb.StringProperty()
    user_id = ndb.StringProperty()
    text = ndb.StringProperty()
    longtitude = ndb.FloatProperty()
    latitude = ndb.FloatProperty()
    date_time = ndb.StringProperty()
    wasRead = ndb.BooleanProperty()


# UserOnChannel nsb class
class UserOnChannel(ndb.Model):
    channel_id = ndb.StringProperty()
    user_id = ndb.StringProperty()


# User ndb class
class User(ndb.Model):
    nick_name = ndb.StringProperty()


####################################################################################
################################                    ################################
################################    Basic Simple Classes   ################################
################################                    ################################
####################################################################################

# Channel simple class ("Channel" name is already taken)
class Chn:
    # Chn constructor
    def __init__(self, id, name, icon):
        self.id = id
        self.name = name
        self.icon = icon


# Channels list simple class
class Channels:
    # Channels object constructor
    def __init__(self, channels):
        self.channels = channels

    # JSON formatter
    def to_JSON(self):
        return json.dumps(self, default=lambda o: o.__dict__, sort_keys=True, indent=4)


# Servers list simple class
class Servers:
    # Servers object constructor
    def __init__(self, servers):
        self.server = servers

    # JSON formatter
    def to_JSON(self):
        return json.dumps(self, default=lambda o: o.__dict__, sort_keys=True, indent=4)


# UserAuthentication simple class
class UserAuthentication:
    # UserAuthentication constructor
    def __init__(self):
        self.status = None
        self.message = None

    # return message for fail in log in
    def report_on_unknown_user(self):
        self.status = '0'
        self.message = "You are not logged in. Please login"
        result_to_return = MessageResultJson(self.status, self.message)
        return result_to_return


# UpdateMethodData simple class
class UpdateMethodData:
    # UpdateMethodData constructor
    def __init__(self, user, action, data):
        self.user = user
        self.action = action
        self.data = data

    # Update the connected servers that an action occurred
    def update_connected_servers(self):
        json_data = json.dumps(self.data)
        params = {"user": self.user, "action": self.action, "data": json_data}
        form_data = urllib.urlencode(params)
        urlfetch.set_default_fetch_deadline(20)
        connected_servers = ndb.gql("SELECT * FROM Server WHERE is_connected=True")
        for server in connected_servers:
            urlfetch.fetch(url="http://" + server.link + "/update",
                           payload=form_data,
                           method=urlfetch.POST,
                           headers={'Content-Type': 'application/x-www-form-urlencoded'})


# MessageResultJson simple class
class MessageResultJson:
    # MessageResultJson constructor
    def __init__(self, status, message):
        self.status = status
        self.message = message

    # JSON formatter
    def to_JSON(self):
        return json.dumps(self, default=lambda o: o.__dict__, sort_keys=True, indent=4)


# UpdateMessages simple class - for GetUpdatesHandler class
class UpdateMessages:
    # UpdateMessages constructor
    def __init__(self, channel, user_id, text, longtitude, latitude, date_time):
        self.channel_id = channel
        self.user_id = user_id
        self.text = text
        self.longtitude = longtitude
        self.latitude = latitude
        self.date_time = date_time


# UpdateMessages simple class
class ReturnedMessagesJson:
    # UpdateMessages constructor
    def __init__(self, messages):
        self.messages = messages

    # JSON formatter
    def to_JSON(self):
        return json.dumps(self, default=lambda o: o.__dict__, sort_keys=True, indent=4)


# UsersOnChannelInternal simple class - for GetNetworkHandler class.
class UsersOnChannelInternal:
    # UsersOnChannelInternal constructor
    def __init__(self, channel_id, members):
        self.id = channel_id
        self.members = members


# UserOnChannelExternal simple class - for GetNetworkHandler class.
class UserOnChannelExternal:
    # UserOnChannelExternal constructor
    def __init__(self, channels):
        self.channels = channels

    # JSON formatter
    def to_JSON(self):
        return json.dumps(self, default=lambda o: o.__dict__, sort_keys=True, indent=4)


########################################################################################
################################                        ################################
################################   Requests  Handlers   ################################
################################                        ################################
########################################################################################

# Get all the servers on the entire network
class GetServersHandler(webapp2.RequestHandler):
    def get(self):
        try:
            # load servers from the servers which connected to our server
            #  and stored them on data store for to including them on the response.
            connected_servers = ndb.gql("SELECT * FROM Server WHERE is_connected=True")
            for server in connected_servers:
                url = "http://" + server.link + "/getServers"
                try:
                    result = urllib2.urlopen(url).read()
                    extract_info = json.loads(result)
                    extract_info = ast.literal_eval(json.dumps(extract_info))
                    servers = extract_info['server']
                    for link in servers:
                        needless_part_of_url = "http://"
                        if needless_part_of_url in link:
                            link = link.replace(needless_part_of_url, "")
                        bad_server1 = "project-ocdi.appspot.com"
                        bad_server2 = "dddd-daniel-1234.appspot.com"
                        our_old_server1 = "chatappserver-985.appspot.com"
                        our_old_server2 = "chatappserver-999.appspot.com"
                        our_old_server3 = "chatappserver-111.appspot.com"
                        our_old_server4 = "chatappserver-222.appspot.com"
                        # check is the server is already in out list, is it is, don't add it.
                        query = Server.query(Server.link == link).get()
                        if query is None and link != "" and  \
                            link != app_identity.get_default_version_hostname() and \
                            link != bad_server1 and link != bad_server2 and link != our_old_server1\
                                and link != our_old_server2 and link != our_old_server3\
                                and link != our_old_server4:
                            Server(link=link, is_connected=False).put()
                except:
                    # On the one hand, we don't want to report on failure in the response
                    #  and don't want to crash on the other hand.
                    pass
            # load the servers which stored in our data store and response list of them.
            query = ndb.gql("SELECT * FROM Server")
            servers = []
            for server in query:
                servers.append(server.link)
            srvs = Servers(servers)
            self.response.write(srvs.to_JSON())
        except:
            self.response.headers['Content-Type'] = 'text/plain'
            self.response.write('Could not complete get servers')
            return


# Send a message in a channel
class SendMessageHandler(webapp2.RequestHandler):
    def post(self):
        # check whether the user approved to us to get his
        user = users.get_current_user()
        user_authentication = UserAuthentication()
        if user is not None:
            user_nickname = str(user.nickname())
        else:
            result_to_return = user_authentication.report_on_unknown_user()
            self.response.write(result_to_return.to_JSON())
            return
        # check whether the user stored in our db
        query = ndb.gql("SELECT * FROM User WHERE nick_name='" + user_nickname + "';")
        if not query.count(limit=1):
            result_to_return = user_authentication.report_on_unknown_user()
            self.response.write(result_to_return.to_JSON())
            return

        # The default of the method is success. In case of failure we change the result.
        status = '1'
        message = ""
        try:
            # check if channel is not empty
            channel_id = self.request.get("channel_id", "")
            if channel_id is "":
                status = '0'
                message = "missing channel id"
                result_to_return = MessageResultJson(status, message)
                self.response.write(result_to_return.to_JSON())
                return
            # check if text is not empty
            text = self.request.get("text", "")
            if text is "":
                status = '0'
                message = "missing text"
                result_to_return = MessageResultJson(status, message)
                self.response.write(result_to_return.to_JSON())
                return
            # check if coordination's are valid
            longtitude = self.request.get("longtitude", "")
            latitude = self.request.get("latitude", "")
            try:
                longtitude = float(longtitude)
                latitude = float(latitude)
            except:
                status = '0'
                message = "bad coordination given"
                result_to_return = MessageResultJson(status, message)
                self.response.write(result_to_return.to_JSON())
                return

            # set current time
            current_date_time = gmtime()
            date_time = strftime("%Y-%m-", current_date_time)
            hour_before_modulo = (int(strftime("%H", current_date_time)) + 3)
            hour_after_modulo = hour_before_modulo % 24
            if hour_after_modulo < hour_before_modulo:
                date_time += str(int(strftime("%d ", current_date_time)) + 1)
            else:
                date_time += str(strftime("%d ", current_date_time))
            date_time += str(hour_after_modulo)
            date_time += strftime(":%M:%S", current_date_time)

            # create Message object to store
            message_to_store = Message(id=date_time,
                                           channel_id=channel_id,
                                           user_id=user_nickname,
                                           text=text,
                                           longtitude=longtitude,
                                           latitude=latitude,
                                           date_time=date_time,
                                           wasRead=False)
            message_to_store.put()
        except:
            status = '0'
            message = "failed to send the message"
            result_to_return = MessageResultJson(status, message)
            self.response.write(result_to_return.to_JSON())
            return

        # update the connected servers
        try:
            data = {"channel": str(channel_id), "text": str(text),
                    "longtitude": longtitude, "latitude": latitude}
            update_method_data = UpdateMethodData(user_nickname, 3, data)
            update_method_data.update_connected_servers()
        except:
            status = 0
            message = "failed to update the connected servers but sending the message succeeded"
            result_to_return = MessageResultJson(status, message)
            self.response.write(result_to_return.to_JSON())
            return

        # report on success
        result_to_return = MessageResultJson(status, message)
        self.response.write(result_to_return.to_JSON())


# Get updates about new messages in my channels
class GetUpdatesHandler(webapp2.RequestHandler):
    def get(self):
        # check whether the user approved to us to get his
        user = users.get_current_user()
        user_authentication = UserAuthentication()
        if user is not None:
            user_nickname = str(user.nickname())
        else:
            result_to_return = user_authentication.report_on_unknown_user()
            self.response.write(result_to_return.to_JSON())
            return
        # check whether the user stored in our db
        query = ndb.gql("SELECT * FROM User WHERE nick_name='" + user_nickname + "';")
        if not query.count(limit=1):
            result_to_return = user_authentication.report_on_unknown_user()
            self.response.write(result_to_return.to_JSON())
            return

        messages_to_return = []
        try:
            # get channels to remove
            to_move = ndb.gql("""SELECT * FROM ChannelToRemove""")
            # get messages posted by other users
            query = ndb.gql("""SELECT * FROM Message""")
            # get my channels
            my_channels = ndb.gql("""SELECT * FROM Channel WHERE is_my = True """)

            # find messages that weren't read and are in my channels
            for message in query:
                if not message.wasRead:
                    for my in my_channels:
                        if my.channel_id == message.channel_id and user_nickname != message.user_id:
                            json_message = UpdateMessages(message.channel_id,
                                                          message.user_id,
                                                          message.text,
                                                          message.longtitude,
                                                          message.latitude,
                                                          message.date_time)
                            messages_to_return.append(json_message)
                            message.wasRead = True
                            message.put()

                            # if channel need to move, get it's link and add it to the response
                            for channel in to_move:
                                if channel.channel_id == message.channel_id:
                                    link = channel.link_to_server
                                    messages_to_return.append(link)
                                    channel.key.delete()

        except:
            self.response.headers['Content-Type'] = 'text/plain'
            self.response.write('Could not complete get updates')
            return

        ret = ReturnedMessagesJson(messages_to_return)
        self.response.write(ret.to_JSON())


# Get all the channels in the network
class GetChannelsHandler(webapp2.RequestHandler):
    def get(self):
        query = ndb.gql("""SELECT * FROM Channel ORDER BY channel_id""")
        channels = []
        for channel in query:
            channels.append(Chn(id=channel.channel_id, name=channel.name, icon=channel.icon))
        chns = Channels(channels)
        self.response.out.write(chns.to_JSON())


# Make a user join to the input channel
class JoinChannelHandler(webapp2.RequestHandler):
    def post(self):
        # get the details of the user
        user = users.get_current_user()
        user_authentication = UserAuthentication()
        if user is not None:
            user_nickname = str(user.nickname())
        else:
            result_to_return = user_authentication.report_on_unknown_user()
            self.response.write(result_to_return.to_JSON())
            return
        # check whether the user stored in our db
        query = ndb.gql("SELECT * FROM User WHERE nick_name='" + user_nickname + "';")
        if not query.count(limit=1):
            result_to_return = user_authentication.report_on_unknown_user()
            self.response.write(result_to_return.to_JSON())
            return

        # The default of the method is success. In case of failure we change the result.
        status = 1
        message = ""

        # check if channel exist
        channel_id = self.request.get("id", "")
        qry = Channel.query(Channel.channel_id == channel_id).get()
        if qry is None:
            status = 0
            message = "Channel does not exist"
            result_to_return = MessageResultJson(status, message)
            self.response.write(result_to_return.to_JSON())
            return

        # Add the user-channel tuple to the UserOnChannel table
        try:
            # for check whether if the user not already found in the given channel
            query = ndb.gql("SELECT * FROM UserOnChannel WHERE channel_id='" + channel_id + "';")
            is_already_in = False
            for user_on_channel in query:
                if user_on_channel.user_id == user_nickname:
                    is_already_in = True
                    break

            if not is_already_in:
                user_on_channel = UserOnChannel(channel_id=channel_id, user_id=user_nickname)
                user_on_channel.put()
                channel = Channel.query(Channel.channel_id == channel_id).get()
                if not channel.is_my:
                    channel.is_my = True
                    channel.put()
            else:
                status = 0
                message = "you are already in this channel"
                result_to_return = MessageResultJson(status, message)
                self.response.write(result_to_return.to_JSON())
                return
        except:
            status = 0
            message = "failed to join to channel"
            result_to_return = MessageResultJson(status, message)
            self.response.write(result_to_return.to_JSON())
            return

        # update the connected servers
        try:
            data = {"channel_id": str(channel_id)}
            update_method_data = UpdateMethodData(user_nickname, 5, data)
            update_method_data.update_connected_servers()
        except:
            status = 0
            message = "failed to update the connected servers but the join succeeded"
            result_to_return = MessageResultJson(status, message)
            self.response.write(result_to_return.to_JSON())
            return
        # report on success
        result_to_return = MessageResultJson(status, message)
        self.response.write(result_to_return.to_JSON())


# Add a new channel
class AddChannelHandler(webapp2.RequestHandler):
    def post(self):
        # check whether the user approved to us to get his
        user = users.get_current_user()
        user_authentication = UserAuthentication()
        if user is not None:
            user_nickname = str(user.nickname())
        else:
            result_to_return = user_authentication.report_on_unknown_user()
            self.response.write(result_to_return.to_JSON())
            return
        # check whether the user stored in our db
        query = ndb.gql("SELECT * FROM User WHERE nick_name='" + user_nickname + "';")
        if not query.count(limit=1):
            result_to_return = user_authentication.report_on_unknown_user()
            self.response.write(result_to_return.to_JSON())
            return

        # The default of the method is success. In case of failure we change the result.
        status = 1
        message = ""
        try:
            channel_id = self.request.get("id", "")
            # for check whether the channel_id is already chose
            query = ndb.gql("SELECT * FROM Channel WHERE channel_id='" + str(channel_id) + "';")
            if channel_id is "":
                status = 0
                message = "missing id"
                result_to_return = MessageResultJson(status, message)
                self.response.write(result_to_return.to_JSON())
                return
            if query.count(limit=1):
                status = 0
                message = "The id you chose is in use. Please select another one."
                result_to_return = MessageResultJson(status, message)
                self.response.write(result_to_return.to_JSON())
                return
            name = self.request.get("name", "")
            if name is "":
                status = 0
                message = "missing name"
                result_to_return = MessageResultJson(status, message)
                self.response.write(result_to_return.to_JSON())
                return
            icon = self.request.get("icon", "")
            if icon is "":
                status = 0
                message = "missing icon"
                result_to_return = MessageResultJson(status, message)
                self.response.write(result_to_return.to_JSON())
                return
            # create Channel entity
            channel = Channel(channel_id=channel_id, name=name, icon=icon, is_my=True)
            channel.put()
            # create UserOnChannel entity - entity which tell that user is on channel
            user_on_channel = UserOnChannel(channel_id=channel_id, user_id=user_nickname)
            user_on_channel.put()
        except:
            status = 0
            message = "Failed to create the channel"
            result_to_return = MessageResultJson(status, message)
            self.response.write(result_to_return.to_JSON())
            return

        # update the connected servers
        try:
            data = {"channel_id": str(channel_id), "name": str(name), "icon": str(icon)}
            update_method_data = UpdateMethodData(user_nickname, 4, data)
            update_method_data.update_connected_servers()
        except:
            status = 0
            message = "failed to update the connected servers but the channel added"
            result_to_return = MessageResultJson(status, message)
            self.response.write(result_to_return.to_JSON())
            return
        # report on success
        result_to_return = MessageResultJson(status, message)
        self.response.write(result_to_return.to_JSON())


# Login to current server
class LoginHandler(webapp2.RequestHandler):
    def get(self):
        # The default of the method is success. In case of failure we change the result.
        status = 1
        message = ""
        user = users.get_current_user()
        if user:
            user_nickname = str(user.nickname())
            # user is authenticate the app but already logged in the server
            query = ndb.gql("SELECT * FROM User WHERE nick_name='" + user_nickname + "';")
            if query.count(limit=1):
                status = 0
                message = "You are already logged in"
                result_to_return = MessageResultJson(status, message)
                self.response.write(result_to_return.to_JSON())
                return
            # this is a new user so we saving it
            try:
                user = User(id=user_nickname,
                            nick_name=user_nickname)
                user.put()
            except:
                status = 0
                message = "failed to login to server"
                result_to_return = MessageResultJson(status, message)
                self.response.write(result_to_return.to_JSON())
                return

            # update the connected servers
            try:
                data = {"server": app_identity.get_default_version_hostname()}
                update_method_data = UpdateMethodData(user_nickname, 1, data)
                update_method_data.update_connected_servers()
            except:
                status = 0
                message = "failed to update the connected servers" + "but the login succeeded"
                result_to_return = MessageResultJson(status, message)

            try:
                # checkout all the channel_id where user_id equal to my
                query = ndb.gql("SELECT * FROM UserOnChannel WHERE user_id='" + user_nickname + "';")
                for user_on_channel in query:
                    channel_query = ndb.gql("SELECT * FROM Channel WHERE channel_id='" + str(user_on_channel.channel_id) + "';")
                    for channel in channel_query:
                        if not channel.is_my:
                            channel.is_my = True
                            channel.put()
            except:
                status = 0
                message = message + "failed to retrieve user channels" + "but the login succeeded"
                result_to_return = MessageResultJson(status, message)
                self.response.write(result_to_return.to_JSON())
                return

            # report that the login succeeded
            result_to_return = MessageResultJson(status, message)
            self.response.write(result_to_return.to_JSON())

        else:
            self.redirect(users.create_login_url('/login'))


# Logoff from current server
class LogoffHandler(webapp2.RequestHandler):
    def get(self):
        # The default of the method is success. In case of failure we change the result.
        status = 1
        message = ""
        user = users.get_current_user()
        user_authentication = UserAuthentication()
        if user:
            user_nickname = str(user.nickname())
        else:
            result_to_return = user_authentication.report_on_unknown_user()
            self.response.write(result_to_return.to_JSON())
            return
        # check whether the user stored in our db
        query = ndb.gql("SELECT * FROM User WHERE nick_name='" + user_nickname + "';")
        if not query.count(limit=1):
            result_to_return = user_authentication.report_on_unknown_user()
            self.response.write(result_to_return.to_JSON())
            return
        try:
            query = ndb.gql("SELECT * FROM User WHERE nick_name='" + user_nickname + "';")
            if query.count(limit=1):
                user = User.query(User.nick_name == user_nickname).get()
                user.key.delete()
        except:
            status = 0
            message = "Failed to remove user"
            result_to_return = MessageResultJson(status, message)
            self.response.write(result_to_return.to_JSON())
            return
        # check the channels after logout, whether there is need to mark channels as not out
        try:
            remove_list = ndb.gql("SELECT * FROM UserOnChannel WHERE user_id='" + user_nickname + "';")
            for userChan in remove_list:
                check_num = ndb.gql("SELECT * FROM UserOnChannel WHERE user_id='" + userChan.user_id + "';")
                if check_num.count() < 2:
                    for channelId in check_num:
                        get_channel = ndb.gql("SELECT * FROM Channel WHERE channel_id='" + str(channelId.channel_id) + "';")
                        for change_chanel in get_channel:
                            if change_chanel.is_my:
                                change_chanel.is_my = False
                                change_chanel.put()
        except:
            status = 0
            message = "failed to update the connected servers"
            result_to_return = MessageResultJson(status, message)
            self.response.write(result_to_return.to_JSON())
            return

        # update the connected servers
        try:
            data = {"server": app_identity.get_default_version_hostname()}
            update_method_data = UpdateMethodData(user_nickname, 2, data)
            update_method_data.update_connected_servers()
        except:
            status = 0
            message = "failed to update the connected servers but the logoff succeeded"
            result_to_return = MessageResultJson(status, message)
            self.response.write(result_to_return.to_JSON())
            return
        # report on success
        result_to_return = MessageResultJson(status, message)
        self.response.write(result_to_return.to_JSON())


# Change the server of the attached channels
class ChangeChannelsHandler(webapp2.RequestHandler):
    # method for load balancing
    def post(self):
        # The default of the method is success. In case of failure we change the result.
        status = 1
        message = ""
        try:
            remove_list = self.request.get("remove", "")
            if remove_list is "":
                status = 0
                message = "missing channels's remove list"
            else:
                remove_list = remove_list.split(",")
                remove_list = [str(string) for string in remove_list]
                link_to_server = self.request.get("linkToServer", "")
                if link_to_server is "":
                    status = 0
                    message = "missing link to server"
            result_to_return = MessageResultJson(status, message)
            if status == 0:
                self.response.write(result_to_return.to_JSON())
                return
            for channel_id in remove_list:
                channel = ndb.gql("SELECT * FROM Channel WHERE channel_id ='" + channel_id + "';").get()
                # mark the channel as channel which don't belong to our server
                if channel.is_my:
                    channel.is_my = False
                    channel.put()
                query = ndb.gql("SELECT * FROM ChannelToRemove WHERE channel_id='" + channel_id + "';")
                # if we didn't asked to remove this channel yet.
                if not query.count(limit=1):
                    channel_to_remove = ChannelToRemove(channel_id=channel_id,
                                                        link_to_server=link_to_server)
                    channel_to_remove.put()
        except:
            status = 0
            message = "failed to change channels"
            result_to_return = MessageResultJson(status, message)
            self.response.write(result_to_return.to_JSON())
            return

        # report on success
        result_to_return = MessageResultJson(status, message)
        self.response.write(result_to_return.to_JSON())


# Update the connected servers that an action occurred (supply the action number and data)
class UpdateHandler(webapp2.RequestHandler):
    def __init__(self, request, response, *args, **kwargs):
        super(UpdateHandler, self).__init__(*args, **kwargs)
        self.data_json = None
        self.user_id = None
        self.result_to_return = None
        self.initialize(request, response)

    # publish message
    def action_three(self):
        try:
            extract_info = json.loads(self.data_json)
            extract_info = ast.literal_eval(json.dumps(extract_info))
            channel_id = extract_info['channel']
            # for check whether the channel is exist
            query = ndb.gql("SELECT * FROM Channel WHERE channel_id='" + channel_id + "';")
            if not query.count(limit=1):
                status = 0
                message = "The channel doesn't exist."
                self.result_to_return = MessageResultJson(status, message)
                return
            channel = query.get()
            # check if the message is for for user which belong to me
            if not channel.is_my:
                status = 0
                message = "The message was not sent for user which belong to me, so I ignore it."
                self.result_to_return = MessageResultJson(status, message)
                return

            # set current time
            current_date_time = gmtime()
            date_time = strftime("%Y-%m-", current_date_time)
            hour_before_modulo = (int(strftime("%H", current_date_time)) + 3)
            hour_after_modulo = hour_before_modulo % 24
            if hour_after_modulo < hour_before_modulo:
                date_time += str(int(strftime("%d ", current_date_time)) + 1)
            else:
                date_time += str(strftime("%d ", current_date_time))
            date_time += str(hour_after_modulo)
            date_time += strftime(":%M:%S", current_date_time)
            text = extract_info['text']
            longtitude = extract_info['longtitude']
            latitude = extract_info['latitude']
            # save the message which accepted from another (user which don't belong to me).
            message_to_store = Message(id=date_time,
                                              channel_id=channel_id,
                                              user_id=self.user_id,
                                              text=text,
                                              longtitude=longtitude,
                                              latitude=latitude,
                                              date_time=date_time,
                                              wasRead=False)
            message_to_store.put()
        except:
            status = 0
            message = "failed to send the message"
            self.result_to_return = MessageResultJson(status, message)

    # create channel
    def action_four(self):
        extract_info = json.loads(self.data_json)
        extract_info = ast.literal_eval(json.dumps(extract_info))
        channel_id = extract_info['channel_id']
        # for check whether the channel is already exist
        query = ndb.gql("SELECT * FROM Channel WHERE channel_id='" + channel_id + "';")
        if query.count(limit=1):
            status = 0
            message = "The id you chose is in use. Please select another one."
            self.result_to_return = MessageResultJson(status, message)
            return
        try:
            name = extract_info['name']
            icon = extract_info['icon']
            # add Channel entity
            channel = Channel(channel_id=channel_id,
                              name=name,
                              icon=icon,
                              is_my=False)
            channel.put()
            # add UserOnChannel entity
            user_on_channel = UserOnChannel(channel_id=channel_id, user_id=self.user_id)
            user_on_channel.put()
        except:
            status = 0
            message = "Failed to create the channel."
            self.result_to_return = MessageResultJson(status, message)

    # join to channel
    def action_five(self):
        try:
            extract_info = json.loads(self.data_json)
            extract_info = ast.literal_eval(json.dumps(extract_info))
            channel_id = extract_info['channel_id']
            # for check whether if the user not already found in the given channel
            query = ndb.gql("SELECT * FROM UserOnChannel WHERE channel_id='" + channel_id + "';")
            is_already_in = False
            for user_on_channel in query:
                if user_on_channel.user_id == self.user_id:
                    is_already_in = True
                    break

            if not is_already_in:
                user_on_channel = UserOnChannel(channel_id=channel_id, user_id=self.user_id)
                user_on_channel.put()
            else:
                status = 0
                message = "you already found in this channel"
                self.result_to_return = MessageResultJson(status, message)
        except:
            status = 0
            message = "failed to join to channel"
            self.result_to_return = MessageResultJson(status, message)

    # leave channel
    def action_six(self):
        try:
            extract_info = json.loads(self.data_json)
            extract_info = ast.literal_eval(json.dumps(extract_info))
            channel_id = extract_info['channel_id']
            query = ndb.gql("SELECT * FROM UserOnChannel WHERE channel_id='" + channel_id + "';")
            is_register = False
            if query.count(limit=1):
                for element in query:
                    if element.user_id == self.user_id:
                        element.key.delete()
                        is_register = True
            if not is_register:
                status = 0
                message = "you are not registering to this channel, so you can't leave it."
                self.result_to_return = MessageResultJson(status, message)

            query = ndb.gql("SELECT * FROM UserOnChannel " +
                            "WHERE channel_id ='" + channel_id + "';")

            if not query.count(limit=1):
                # there is no users in the channel, so need to delete it
                channel = Channel.query(Channel.channel_id == channel_id).get()
                channel.key.delete()
        except:
            status = 0
            message = "failed to leave channel"
            self.result_to_return = MessageResultJson(status, message)

    # delete channel
    def action_seven(self):
        try:
            extract_info = json.loads(self.data_json)
            extract_info = ast.literal_eval(json.dumps(extract_info))
            channel_id = extract_info['channel_id']
            # check whether there is that channel
            query = ndb.gql("SELECT * FROM Channel WHERE channel_id='" + channel_id + "';")
            if not query.count(limit=1):
                status = 0
                message = "there is not channel with that id, so there is no what to delete."
                self.result_to_return = MessageResultJson(status, message)
                return
            query2 = ndb.gql("SELECT * FROM UserOnChannel WHERE channel_id='" + channel_id + "';")
            if query2.count(limit=1):
                # delete the user from the channel
                is_register = False
                if query.count(limit=1):
                    for element in query2:
                        if element.user_id == self.user_id:
                            element.key.delete()
                            is_register = True
                if not is_register:
                    status = 0
                    message = "you are not registering to this channel, so you can't leave it."
                    self.result_to_return = MessageResultJson(status, message)
                    return
                # delete the channel
                for channel in query:
                    channel.key.delete()
        except:
            status = 0
            message = "failed to delete channel"
            self.result_to_return = MessageResultJson(status, message)

    # handle in the request
    def post(self):
        # The default of the method is success. In case of failure we change the result.
        status = 1
        message = ""
        self.result_to_return = MessageResultJson(status, message)
        try:
            user = self.request.get("user", "")
            if user is "":
                status = 0
                message = "missing user"
            else:
                action = self.request.get("action", "")
                if action is "":
                    status = 0
                    message = "missing action"
                else:
                    data = self.request.get("data", "")
                    if data is "":
                        status = 0
                        message = "missing data"
            self.result_to_return = MessageResultJson(status, message)
            if status == 0:
                self.response.write(self.result_to_return.to_JSON())
                return
        except:
            status = 0
            message = "failed to get the update"
            self.result_to_return = MessageResultJson(status, message)
            self.response.write(self.result_to_return.to_JSON())
            return

        # Check whether the user is connecting to me. If this is so, we have a circle.
        # Therefore I don't use in this information and don't deliver it on.
        query = ndb.gql("SELECT * FROM User WHERE nick_name='" + str(user) + "'; ")
        if query.count(limit=1):
            status = 0
            message = "This is a circle, I delivered this information. So, I stop the circling."
            self.result_to_return = MessageResultJson(status, message)
            self.response.write(self.result_to_return.to_JSON())
            return
        # update the connected servers
        try:
            update_method_data = UpdateMethodData(str(user), action, data)
            update_method_data.update_connected_servers()
        except:
            status = 0
            message = "failed to update the connected servers"
            self.result_to_return = MessageResultJson(status, message)
        self.data_json = data
        self.user_id = str(user)
        # map the inputs to the function blocks, and so handle in the given request action.
        # login('1') and logoff ('2') don't get an handle because in our implementation
        # of the server and it's data store we don't have what to do with this information.
        options = {
            '3': self.action_three,
            '4': self.action_four,
            '5': self.action_five,
            '6': self.action_six,
            '7': self.action_seven
        }
        options[str(action)]()
        # report the result
        self.response.write(self.result_to_return.to_JSON())


# Register the given server to this server and vice versa
class RegisterHandler(webapp2.RequestHandler):
    def post(self):
        # The default of the method is success. In case of failure we change the result.
        status = 1
        message = ""
        try:
            # check if link is not empty
            link = self.request.get("link", "")
            if link is "":
                status = 0
                message = "missing link to server"
                result_to_return = MessageResultJson(status, message)
                self.response.write(result_to_return.to_JSON())
                return
            needless_part_of_url = "http://"
            if needless_part_of_url in link:
                link = link.replace(needless_part_of_url, "")

            if link == app_identity.get_default_version_hostname():
                status = 0
                message = "You cannot register myself"
                result_to_return = MessageResultJson(status, message)
                self.response.write(result_to_return.to_JSON())
                return

            our_old_server1 = "chatappserver-985.appspot.com"
            our_old_server2 = "chatappserver-999.appspot.com"
            our_old_server3 = "chatappserver-111.appspot.com"
            our_old_server4 = "chatappserver-222.appspot.com"
            if link == our_old_server1 or link == our_old_server2 \
                    or link == our_old_server3 or link == our_old_server4:
                status = 0
                message = "You cannot register my old server which not in use"
                result_to_return = MessageResultJson(status, message)
                self.response.write(result_to_return.to_JSON())
                return
            our_backup_server1 = "chatappserver-bu1.appspot.com"
            our_backup_server2 = "chatappserver-bu2.appspot.com"
            our_backup_server3 = "chatappserver-bu3.appspot.com"
            if link == our_backup_server1 or link == our_backup_server2 \
                    or link == our_backup_server3:
                status = 0
                message = "You cannot register my backup server"
                result_to_return = MessageResultJson(status, message)
                self.response.write(result_to_return.to_JSON())
                return
            server = Server(link=link, is_connected=True)

            # get list of connected servers
            query = ndb.gql("""SELECT * FROM Server WHERE is_connected=True""")

            # check if there are already 3 servers
            if query.count() == 3:
                status = 0
                message = "server limit exceeded"
                result_to_return = MessageResultJson(status, message)
                self.response.write(result_to_return.to_JSON())
                return

            query = ndb.gql("SELECT * FROM Server WHERE link='" + link + "';")
            if query.count(limit=1):
                server2 = query.get()
                # check if server is already registered
                if server2.is_connected:
                    status = 0
                    message = "server already registered"
                    result_to_return = MessageResultJson(status, message)
                    self.response.write(result_to_return.to_JSON())
                    return
                # the server is familier to the us, but don't connect to us.
                server2.is_connected = True
                server2.put()
                # report on success
                result_to_return = MessageResultJson(status, message)
                self.response.write(result_to_return.to_JSON())
                return
         # if there are less than 3, and server not registered, add it
            server.put()
        except:
            status = 0
            message = "failed to register server"
            result_to_return = MessageResultJson(status, message)
            self.response.write(result_to_return.to_JSON())
            return

        # form a 2-way connection with the server
        if "http" in link:
            url = link + "/register"
        else:
            url = "http://" + link + "/register"
        form_fields = {"link": app_identity.get_default_version_hostname()}
        form_data = urllib.urlencode(form_fields)
        urlfetch.fetch(url=url,
                       payload=form_data,
                       method=urlfetch.POST,
                       headers={'Content-Type': 'application/x-www-form-urlencoded'})

        # report on success
        result_to_return = MessageResultJson(status, message)
        self.response.write(result_to_return.to_JSON())


# UnRegister the given server to this server and vice versa
class UnRegisterHandler(webapp2.RequestHandler):
    def post(self):
        # The default of the method is success. In case of failure we change the result.
        status = 1
        message = ""
        try:
            # check if link is not empty
            link = self.request.get("link", "")
            if link is "":
                status = 0
                message = "missing link to server"
                result_to_return = MessageResultJson(status, message)
                self.response.write(result_to_return.to_JSON())
                return

            # check if there is a server with the input link
            server = Server.query(Server.link == link).get()
            if server is None:
                status = 0
                message = "server not registered"
                result_to_return = MessageResultJson(status, message)
                self.response.write(result_to_return.to_JSON())
                return
            # if there is a registered server unregister it
            else:
                if server.is_connected:
                    server.is_connected = False
                    server.put()
        except:
            status = 0
            message = "failed to unregister the server"
            result_to_return = MessageResultJson(status, message)
            self.response.write(result_to_return.to_JSON())
            return

        try:
            # form a 2-way connection with the server
            url = "http://" + link + "/unRegister"
            form_fields = {"link": app_identity.get_default_version_hostname()}
            form_data = urllib.urlencode(form_fields)
            urlfetch.fetch(url=url,
                           payload=form_data,
                           method=urlfetch.POST,
                           headers={'Content-Type': 'application/x-www-form-urlencoded'})
        except:
            status = 0
            message = "failed to unregister from server"
            result_to_return = MessageResultJson(status, message)
            self.response.write(result_to_return.to_JSON())
            return
        # report on success
        result_to_return = MessageResultJson(status, message)
        self.response.write(result_to_return.to_JSON())


# Make the user leave the attached channel
class LeaveChannelHandler(webapp2.RequestHandler):
    def post(self):
        # get the details of the user
        user = users.get_current_user()
        user_authentication = UserAuthentication()
        if user is not None:
            user_nickname = str(user.nickname())
        else:
            result_to_return = user_authentication.report_on_unknown_user()
            self.response.write(result_to_return.to_JSON())
            return
        # check whether the user stored in our db
        query = ndb.gql("SELECT * FROM User WHERE nick_name='" + user_nickname + "';")
        if not query.count(limit=1):
            result_to_return = user_authentication.report_on_unknown_user()
            self.response.write(result_to_return.to_JSON())
            return

        # The default of the method is success. In case of failure we change the result.
        status = 1
        message = ""
        try:
            channel_id = self.request.get("id", "")
            if channel_id is "":
                status = '0'
                message = "missing id"
                result_to_return = MessageResultJson(status, message)
                self.response.write(result_to_return.to_JSON())
                return
            # delete the user from the channel
            query = ndb.gql("SELECT * FROM UserOnChannel WHERE channel_id='" + channel_id + "';")
            is_register = False
            if query.count(limit=1):
                for element in query:
                    if element.user_id == user_nickname:
                        element.key.delete()
                        is_register = True
            if not is_register:
                status = 0
                message = "you are not registering to this channel, so you can't leave it."
                result_to_return = MessageResultJson(status, message)
                self.response.write(result_to_return.to_JSON())
                return

            query = ndb.gql("SELECT * FROM UserOnChannel " +
                            "WHERE channel_id ='" + channel_id + "';")

            if not query.count(limit=1):
                # there is no users in the channel, so need to delete it
                query = ndb.gql("SELECT * FROM Channel WHERE channel_id='" + channel_id + "';")
                for channel in query:
                    channel.key.delete()
                # update the connected servers
                try:
                    data = {"channel_id": str(channel_id)}
                    update_method_data = UpdateMethodData(user_nickname, 7, data)
                    update_method_data.update_connected_servers()
                except:
                    status = 0
                    message = "failed to update the connected servers but the leaving succeeded"
                    result_to_return = MessageResultJson(status, message)
                    self.response.write(result_to_return.to_JSON())
                    return
        except:
            status = 0
            message = "failed to leave channel"
            result_to_return = MessageResultJson(status, message)
            self.response.write(result_to_return.to_JSON())

        # update the connected servers
        try:
            data = {"channel_id": str(channel_id)}
            update_method_data = UpdateMethodData(user_nickname, 6, data)
            update_method_data.update_connected_servers()
        except:
            status = 0
            message = "failed to update the connected servers but the leaving succeeded"
            result_to_return = MessageResultJson(status, message)
            self.response.write(result_to_return.to_JSON())
            return
        # report on success
        result_to_return = MessageResultJson(status, message)
        self.response.write(result_to_return.to_JSON())


# Get list of channels and the users in each channel on the entire network
class GetNetworkHandler(webapp2.RequestHandler):
    def get(self):
        query = ndb.gql("SELECT * FROM UserOnChannel")
        users_list = []
        part_user = []
        channels_id_list = []
        for user_channel in query:
            channel_id = user_channel.channel_id
            # if we didn't handle this channel already
            if not channel_id in channels_id_list:
                channels_id_list.append(channel_id)
                some_users = ndb.gql("SELECT * FROM UserOnChannel WHERE channel_id='" + channel_id + "';")
                for one_user in some_users:
                    part_user.append (one_user.user_id)
                users_list.append(UsersOnChannelInternal(channel_id, part_user))
                part_user = []
        channels = UserOnChannelExternal(users_list)
        self.response.out.write(channels.to_JSON())


# Get the number of clients in the attached channel on current server
class GetNumOfClientsHandler(webapp2.RequestHandler):
    # get num of clients in the given channel on our server
    def post(self):
        try:
            channel_id = self.request.get("id", "")
            if channel_id is "":
                status = '0'
                message = "missing id"
                result_to_return = MessageResultJson(status, message)
                self.response.write(result_to_return.to_JSON())
                return
            query = ndb.gql("SELECT * FROM UserOnChannel WHERE channel_id='" + channel_id + "';")
            clients_counter = 0
            # if the channel is exist
            if query.count(limit=1):
                for user in query:
                    query2 = ndb.gql("SELECT * FROM User WHERE nick_name='" + user.user_id + "';")
                    # if the user was logged in to our server
                    if query2.count(limit=1):
                        clients_counter += 1
                self.response.write(clients_counter)
        except:
            self.response.write("failed to calculate num of clients")


# Get the channels in the current server
class GetMyChannelsHandler(webapp2.RequestHandler):
    def get(self):
        try:
            query = ndb.gql("""SELECT * FROM Channel WHERE is_my = True""")
            channels = []
            for channel in query:
                channels.append(channel.channel_id)
            chns = Channels(channels)
            self.response.out.write(chns.to_JSON())
        except:
            self.response.headers['Content-Type'] = 'text/plain'
            self.response.write('Could not complete get my channels')


# Divide the load of the network between the servers
class LoadBalancerHandler(webapp2.RequestHandler):
    def get(self):
        status = 1
        where_to = ""
        counter = 0

        my_channels = ndb.gql("""SELECT * FROM Channel WHERE is_my = True""")
        connected_servers = ndb.gql("""SELECT * FROM Server WHERE is_connected = True""")

        to_remove = []

        # check if channel need to be changed
        # compare my channels against connected servers channels
        # if the id's are similar check the numer of users
        # if the ratio is larger than 1:3, this channel need to be moved
        try:
            for server in connected_servers:
                server_link = server.link
                url = "http://" + server.link + "/getMyChannels"

                result = urlfetch.fetch(url)
                extract_info = json.loads(result.content)
                other_server_channels = extract_info['channels']

                for my in my_channels:
                    for his in other_server_channels:
                        if my.channel_id == his:
                            # get my num of clients for the channel
                            params = {"id": my.channel_id}
                            form_data = urllib.urlencode(params)
                            urlfetch.set_default_fetch_deadline(20)
                            my_num = urlfetch.fetch(url="http://" + app_identity.get_default_version_hostname()
                                                        + "/getNumOfClients",
                                               payload=form_data,
                                               method=urlfetch.POST,
                                               headers={'Content-Type': 'application/x-www-form-urlencoded'}).content
                            # Get other server num of clients for the channel
                            params2 = {"id": his}
                            form_data2 = urllib.urlencode(params2)
                            urlfetch.set_default_fetch_deadline(20)
                            his_num = urlfetch.fetch(url="http://" + server_link + "/getNumOfClients",
                                               payload=form_data2,
                                               method=urlfetch.POST,
                                               headers={'Content-Type': 'application/x-www-form-urlencoded'}).content
                            # check if this channel members should be transferred
                            if 0 < my_num*3 < his_num:
                                to_remove.append(my.channel_id)
                                where_to = server.link
                                counter += 1
                            else:
                                counter -= 1

                            # if more than half the shared channels should be moved, move the users to the other server
                            if counter > 0:
                                where_to = server.link
                                break

        except:
            status = -2

        # Activate changeChannels method
        try:
            params3 = {"remove": to_remove, "linkToServer": where_to}
            form_data3 = urllib.urlencode(params3)
            urlfetch.set_default_fetch_deadline(20)
            change = urlfetch.fetch(url="http://" + app_identity.get_default_version_hostname() + "/changeChannels",
                                payload=form_data3,
                                method=urlfetch.POST,
                                headers={'Content-Type': 'application/x-www-form-urlencoded'})

        except:
            status = -1

        # return url of the new server
        self.response.write(where_to)



############################################################################
################################			################################
################################	Main	################################
################################			################################
############################################################################

# Match a url request into an appropriate handler
app = webapp2.WSGIApplication([
    ('/getServers', GetServersHandler),
    ('/sendMessage', SendMessageHandler),
    ('/getUpdates', GetUpdatesHandler),
    ('/getChannels', GetChannelsHandler),
    ('/joinChannel', JoinChannelHandler),
    ('/addChannel', AddChannelHandler),
    ('/login', LoginHandler),
    ('/logoff', LogoffHandler),
    ('/changeChannels', ChangeChannelsHandler),
    ('/update', UpdateHandler),
    ('/register', RegisterHandler),
    ('/unRegister', UnRegisterHandler),
    ('/leaveChannel', LeaveChannelHandler),
    ('/getNetwork', GetNetworkHandler),
    ('/getNumOfClients', GetNumOfClientsHandler),
    ('/getMyChannels', GetMyChannelsHandler),
    ('/loadBalancing', LoadBalancerHandler)
], debug=True)
