import { JsonRpcClient } from 'JsonRpcClient';
import { Participant } from 'Participant';

export class Room {

    private roomId: string;
    private participants: { [key: string]: Participant; } = {};
    
    private client: JsonRpcClient;   
   
    constructor() {

        let config = {

            heartbeat: 3000,
            sendCloseMessage: true,
            ws: {
                uri: 'wss://' + location.host + '/room',
                useSockJS: false,
                /*onconnected: connectCallback,
                ondisconnect: disconnectCallback,
                onreconnecting: reconnectingCallback,
                onreconnected: reconnectedCallback*/
            },
            rpc: {
                requestTimeout: 15000,
                //notifications
                iceCandidate: this.iceCandidate.bind(this),
                newParticipantArrived: this.newParticipantArrived.bind(this),
                participantLeft: this.participantLeft.bind(this)
            }
        };

        this.client = new JsonRpcClient(config);
        
        window.onbeforeunload = function() {
            this.client.close();
        };
    }

    joinRoom(roomId: string, participantId: string, callback?: (error:any) => void) {
        
        this.roomId = roomId;

        this.client.exec('joinRoom', { roomId, participantId }, (error, response) => {

            if (error) { 
                if(callback) { 
                    callback(error) 
                } else {
                    console.error(error);    
                }; 
                return; 
            }

            console.log(participantId + " registered in room " + this.roomId);

            let participant = new Participant(participantId, this.client);

            this.participants[participantId] = participant;

            participant.createWebRtcPeerToSend();
            
            response.value.forEach(participantId => this.addRemoteParticipant(participantId));
            
            if(callback){
                callback(undefined);
            }
        });
    }
    
    close() {
        
        this.client.exec('leaveRoom', {}, (error, response) => {
            
            for (let id in this.participants) {
                this.participants[id].dispose();
            }
            
            this.client.close();    
        });        
    }
    
    private addRemoteParticipant(participantId: string) {
        
        console.log("Remote participant " + participantId + " added in room " + this.roomId);
        
        let participant = new Participant(participantId, this.client);
        
        this.participants[participantId] = participant;
        
        participant.createWebRtcPeerToReceive();
    }   

    // JsonRpcMethod    
    iceCandidate(request: any) {
        let participant = this.participants[request.participantId];
        participant.addRemoteIceCandidate(request.iceCandidate);
    }

    // JsonRpcMethod
    participantLeft(request: any) {

        let participantId: string = request.participantId;
        
        console.log('Participant ' + participantId + ' left');

        let participant = this.participants[participantId];
        participant.dispose();

        delete this.participants[participantId];
    }

    // JsonRpcMethod
    newParticipantArrived(request: any) {
        this.addRemoteParticipant(request.participantId);
    }
}
