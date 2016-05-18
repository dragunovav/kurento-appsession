import { JsonRpcClient } from './JsonRpcClient';

declare var kurentoUtils: any;

export class Participant {

    private container: HTMLDivElement;
    private span: HTMLSpanElement;
    private video: HTMLVideoElement;
    private rtcPeer: any;

    constructor(private id: string, private client: JsonRpcClient) {

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

    getElement() {
        return this.container;
    }

    getVideoElement() {
        return this.video;
    }

    createWebRtcPeerToSend() {

        let options = {
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
        }

        this.rtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendonly(options, 
            error => this.negotiateRtcPeer(error));
    }

    createWebRtcPeerToReceive() {

        let options = {
            remoteVideo: this.video,
            onicecandidate: this.onLocalIceCandidate.bind(this)
        }

        this.rtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options, 
            error => this.negotiateRtcPeer(error));
    }

    private negotiateRtcPeer(error: any) {

        if (error) {
            return console.error('Error creating WebRtcPeer: ' + error);
        }

        this.rtcPeer.generateOffer((error: any, sdpOffer: string) => {

            if (error) {
                return console.error("Sdp generate offer error: " + error);
            }

            console.log('Invoking SDP offer callback function');

            this.client.exec('negotiateMediaFor', { participantId: this.id, sdpOffer }, (error, response) => {
             
                if (error) {
                   return console.error("Error executing 'negotiateMediaFor' jsonrpc method: "+error);
                }
                
                this.rtcPeer.processAnswer(response.value, error => {
                    if (error) return console.error("Error in rtcPeer.processAnswer: "+error);
                });
            
            });
        });
    }

    addRemoteIceCandidate(iceCandidate: any) {
        this.rtcPeer.addIceCandidate(iceCandidate, error => {
            if (error) {
                console.error("Error adding candidate: " + error);
            }
        });
    }

    onLocalIceCandidate(iceCandidate: any) {

        console.log("Local candidate " + JSON.stringify(iceCandidate));

        this.client.exec('onRemoteIceCandidate', { iceCandidate, participantId: this.id });
    }

    dispose() {

        console.log('Disposing participant ' + this.id);

        this.rtcPeer.dispose();
        this.container.parentNode.removeChild(this.container);
    }
}
