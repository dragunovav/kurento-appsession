import { Room } from 'Room';

let room = new Room();

export function joinRoom() {
    
    let participantId = <HTMLInputElement> document.getElementById('name');
    let roomId = <HTMLInputElement> document.getElementById('roomName');

    document.getElementById('room-header').innerText = 'ROOM ' + room;
    document.getElementById('join').style.display = 'none';
    document.getElementById('room').style.display = 'block';

    room.joinRoom(roomId.value, participantId.value);
}

export function leaveRoom() {
    
    room.close();
    room = new Room();

    document.getElementById('join').style.display = 'block';
    document.getElementById('room').style.display = 'none';

}