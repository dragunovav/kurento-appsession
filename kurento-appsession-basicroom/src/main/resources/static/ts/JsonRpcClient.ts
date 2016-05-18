declare var RpcBuilder: any;

export class JsonRpcClient {

    private client: any;
    
    constructor(config: any) {
        this.client = new RpcBuilder.clients.JsonRpcClient(config);
    }

    exec(method: string, params: any, callback?: (error: any, response: any) => void) {
        this.client.send(method, params, callback);
    }
    
    close(){
        this.client.close();    
    }
}