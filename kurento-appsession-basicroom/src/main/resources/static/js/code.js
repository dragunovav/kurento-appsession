System.register("JsonRpcClient", [], function(exports_1, context_1) {
    "use strict";
    var __moduleName = context_1 && context_1.id;
    var JsonRpcClient;
    return {
        setters:[],
        execute: function() {
            JsonRpcClient = (function () {
                function JsonRpcClient(config) {
                    this.client = new RpcBuilder.clients.JsonRpcClient(config);
                }
                JsonRpcClient.prototype.exec = function (method, params, callback) {
                    this.client.send(method, params, callback);
                };
                JsonRpcClient.prototype.close = function () {
                    this.client.close();
                };
                return JsonRpcClient;
            }());
            exports_1("JsonRpcClient", JsonRpcClient);
        }
    }
});
System.register("Participant", [], function(exports_2, context_2) {
    "use strict";
    var __moduleName = context_2 && context_2.id;
    var Participant;
    return {
        setters:[],
        execute: function() {
            Participant = (function () {
                function Participant(id, client) {
                    this.id = id;
                    this.client = client;
                    this.container = document.createElement('div');
                    this.container.className = 'participant';
                    this.container.id = id;
                    this.span = document.createElement('span');
                    this.video = document.createElement('video');
                    this.container.appendChild(this.video);
                    this.container.appendChild(this.span);
                    document.getElementById('participants').appendChild(this.container);
                    this.span.appendChild(document.createTextNode(id));
                    this.video.id = 'video-' + id;
                    this.video.autoplay = true;
                    this.video.controls = false;
                }
                Participant.prototype.getElement = function () {
                    return this.container;
                };
                Participant.prototype.getVideoElement = function () {
                    return this.video;
                };
                Participant.prototype.createWebRtcPeerToSend = function () {
                    var _this = this;
                    var options = {
                        localVideo: this.video,
                        mediaConstraints: {
                            audio: true,
                            video: {
                                mandatory: {
                                    maxWidth: 320,
                                    maxFrameRate: 15,
                                    minFrameRate: 15
                                }
                            }
                        },
                        onicecandidate: this.onLocalIceCandidate.bind(this)
                    };
                    this.rtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendonly(options, function (error) { return _this.negotiateRtcPeer(error); });
                };
                Participant.prototype.createWebRtcPeerToReceive = function () {
                    var _this = this;
                    var options = {
                        remoteVideo: this.video,
                        onicecandidate: this.onLocalIceCandidate.bind(this)
                    };
                    this.rtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options, function (error) { return _this.negotiateRtcPeer(error); });
                };
                Participant.prototype.negotiateRtcPeer = function (error) {
                    var _this = this;
                    if (error) {
                        return console.error('Error creating WebRtcPeer: ' + error);
                    }
                    this.rtcPeer.generateOffer(function (error, sdpOffer) {
                        if (error) {
                            return console.error("Sdp generate offer error: " + error);
                        }
                        console.log('Invoking SDP offer callback function');
                        _this.client.exec('negotiateMediaFor', { participantId: _this.id, sdpOffer: sdpOffer }, function (error, response) {
                            if (error) {
                                return console.error("Error executing 'negotiateMediaFor' jsonrpc method: " + error);
                            }
                            _this.rtcPeer.processAnswer(response.value, function (error) {
                                if (error)
                                    return console.error("Error in rtcPeer.processAnswer: " + error);
                            });
                        });
                    });
                };
                Participant.prototype.addRemoteIceCandidate = function (iceCandidate) {
                    this.rtcPeer.addIceCandidate(iceCandidate, function (error) {
                        if (error) {
                            console.error("Error adding candidate: " + error);
                        }
                    });
                };
                Participant.prototype.onLocalIceCandidate = function (iceCandidate) {
                    console.log("Local candidate " + JSON.stringify(iceCandidate));
                    this.client.exec('onRemoteIceCandidate', { iceCandidate: iceCandidate, participantId: this.id });
                };
                Participant.prototype.dispose = function () {
                    console.log('Disposing participant ' + this.id);
                    this.rtcPeer.dispose();
                    this.container.parentNode.removeChild(this.container);
                };
                return Participant;
            }());
            exports_2("Participant", Participant);
        }
    }
});
System.register("Room", ["JsonRpcClient", "Participant"], function(exports_3, context_3) {
    "use strict";
    var __moduleName = context_3 && context_3.id;
    var JsonRpcClient_1, Participant_1;
    var Room;
    return {
        setters:[
            function (JsonRpcClient_1_1) {
                JsonRpcClient_1 = JsonRpcClient_1_1;
            },
            function (Participant_1_1) {
                Participant_1 = Participant_1_1;
            }],
        execute: function() {
            Room = (function () {
                function Room() {
                    this.participants = {};
                    var config = {
                        heartbeat: 3000,
                        sendCloseMessage: true,
                        ws: {
                            uri: 'wss://' + location.host + '/room',
                            useSockJS: false,
                        },
                        rpc: {
                            requestTimeout: 15000,
                            //notifications
                            iceCandidate: this.iceCandidate.bind(this),
                            newParticipantArrived: this.newParticipantArrived.bind(this),
                            participantLeft: this.participantLeft.bind(this)
                        }
                    };
                    this.client = new JsonRpcClient_1.JsonRpcClient(config);
                    window.onbeforeunload = function () {
                        this.client.close();
                    };
                }
                Room.prototype.joinRoom = function (roomId, participantId, callback) {
                    var _this = this;
                    this.roomId = roomId;
                    this.client.exec('joinRoom', { roomId: roomId, participantId: participantId }, function (error, response) {
                        if (error) {
                            if (callback) {
                                callback(error);
                            }
                            else {
                                console.error(error);
                            }
                            ;
                            return;
                        }
                        console.log(participantId + " registered in room " + _this.roomId);
                        var participant = new Participant_1.Participant(participantId, _this.client);
                        _this.participants[participantId] = participant;
                        participant.createWebRtcPeerToSend();
                        response.value.forEach(function (participantId) { return _this.addRemoteParticipant(participantId); });
                        if (callback) {
                            callback(undefined);
                        }
                    });
                };
                Room.prototype.close = function () {
                    var _this = this;
                    this.client.exec('leaveRoom', {}, function (error, response) {
                        for (var id in _this.participants) {
                            _this.participants[id].dispose();
                        }
                        _this.client.close();
                    });
                };
                Room.prototype.addRemoteParticipant = function (participantId) {
                    console.log("Remote participant " + participantId + " added in room " + this.roomId);
                    var participant = new Participant_1.Participant(participantId, this.client);
                    this.participants[participantId] = participant;
                    participant.createWebRtcPeerToReceive();
                };
                // JsonRpcMethod    
                Room.prototype.iceCandidate = function (request) {
                    var participant = this.participants[request.participantId];
                    participant.addRemoteIceCandidate(request.iceCandidate);
                };
                // JsonRpcMethod
                Room.prototype.participantLeft = function (request) {
                    var participantId = request.participantId;
                    console.log('Participant ' + participantId + ' left');
                    var participant = this.participants[participantId];
                    participant.dispose();
                    delete this.participants[participantId];
                };
                // JsonRpcMethod
                Room.prototype.newParticipantArrived = function (request) {
                    this.addRemoteParticipant(request.participantId);
                };
                return Room;
            }());
            exports_3("Room", Room);
        }
    }
});
System.register("Main", ["Room"], function(exports_4, context_4) {
    "use strict";
    var __moduleName = context_4 && context_4.id;
    var Room_1;
    var room;
    function joinRoom() {
        var participantId = document.getElementById('name');
        var roomId = document.getElementById('roomName');
        document.getElementById('room-header').innerText = 'ROOM ' + room;
        document.getElementById('join').style.display = 'none';
        document.getElementById('room').style.display = 'block';
        room.joinRoom(roomId.value, participantId.value);
    }
    exports_4("joinRoom", joinRoom);
    function leaveRoom() {
        room.close();
        room = new Room_1.Room();
        document.getElementById('join').style.display = 'block';
        document.getElementById('room').style.display = 'none';
    }
    exports_4("leaveRoom", leaveRoom);
    return {
        setters:[
            function (Room_1_1) {
                Room_1 = Room_1_1;
            }],
        execute: function() {
            room = new Room_1.Room();
        }
    }
});
//# sourceMappingURL=code.js.map