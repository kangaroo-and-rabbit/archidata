/** @file
 * @author Edouard DUPIN
 * @copyright 2024, Edouard DUPIN, all right reserved
 * @license MPL-2
 */

import { RestErrorResponse } from "./model"

export enum HTTPRequestModel {
    DELETE = 'DELETE',
    GET = 'GET',
    PATCH = 'PATCH',
    POST = 'POST',
    PUT = 'PUT',
}
export enum HTTPMimeType {
    ALL = '*/*',
    CSV = 'text/csv',
    IMAGE = 'image/*',
    IMAGE_JPEG = 'image/jpeg',
    IMAGE_PNG = 'image/png',
    JSON = 'application/json',
    MULTIPART = 'multipart/form-data',
    OCTET_STREAM = 'application/octet-stream',
    TEXT_PLAIN = 'text/plain',
}

export interface RESTConfig {
    // base of the server: http(s)://my.server.org/plop/api/
    server: string;
    // Token to access of the data.
    token?: string;
}

export interface RESTModel {
    // base of the local API request: "sheep/{id}".
    endPoint: string;
    // Type of the request.
    requestType?: HTTPRequestModel;
    // Input type requested.
    accept?: HTTPMimeType;
    // Content of the local data.
    contentType?: HTTPMimeType;
    // Mode of the TOKEN in urk or Header
    tokenInUrl?: boolean;
}

export interface ModelResponseHttp {
    status: number;
    data: any;
}

export function isArrayOf<TYPE>(
    data: any,
    typeChecker: (subData: any) => subData is TYPE,
    length?: number
): data is TYPE[] {
    if (!Array.isArray(data)) {
        return false;
    }
    if (!data.every(typeChecker)) {
        return false;
    }
    if (length !== undefined && data.length != length) {
        return false;
    }
    return true;
}

function isNullOrUndefined(data: any): data is undefined | null {
    return data === undefined || data === null;
}

export type RESTRequestType = {
    restModel: RESTModel,
    restConfig: RESTConfig,
    data?: any,
    params?: object,
    queries?: object,
    progress?: ProgressCallback,
};

function removeTrailingSlashes(input: string): string {
    if (isNullOrUndefined(input)) {
        return "undefined";
    }
    return input.replace(/\/+$/, '');
}
function removeLeadingSlashes(input: string): string {
    if (isNullOrUndefined(input)) {
        return "";
    }
    return input.replace(/^\/+/, '');
}

export function RESTUrl({ restModel, restConfig, params, queries }: RESTRequestType): string {
    // Create the URL PATH:
    let generateUrl = `${removeTrailingSlashes(restConfig.server)}/${removeLeadingSlashes(restModel.endPoint)}`;
    if (params !== undefined) {
        for (let key of Object.keys(params)) {
            generateUrl = generateUrl.replaceAll(`{${key}}`, `${params[key]}`);
        }
    }
    if (queries === undefined && (restConfig.token === undefined || restModel.tokenInUrl !== true)) {
        return generateUrl;
    }
    const searchParams = new URLSearchParams();
    if (queries !== undefined) {
        for (let key of Object.keys(queries)) {
            const value = queries[key];
            if (Array.isArray(value)) {
                for (let iii = 0; iii < value.length; iii++) {
                    searchParams.append(`${key}`, `${value[iii]}`);
                }
            } else {
                searchParams.append(`${key}`, `${value}`);
            }
        }
    }
    if (restConfig.token !== undefined && restModel.tokenInUrl === true) {
        searchParams.append('Authorization', `Bearer ${restConfig.token}`);
    }
    return generateUrl + "?" + searchParams.toString();
}


export type ProgressCallback = (count: number, total: number) => void;
// input: RequestInfo | URL, init?: RequestInit): Promise<Response>;
export function fetchProgress<TYPE>(generateUrl: string, { method, headers, body }: {
    method: HTTPRequestModel,
    headers: any,
    body: any,
}, progress: ProgressCallback): Promise<Response> {

    //async function fetchForm(form, options = {}) {
    const action = generateUrl;
    const data = body;
    const xhr = new XMLHttpRequest();
    return new Promise((resolve, reject) => {
        xhr.responseType = 'blob';
        xhr.onreadystatechange = () => {
            if (xhr.readyState != 4) {
                // done
                return;
            }
            const response = new Response(xhr.response, {
                status: xhr.status,
                statusText: xhr.statusText
            });
            resolve(response);
        }
        // If fail:
        xhr.addEventListener('error', () => {
            reject(new TypeError('Failed to fetch'))
        });
        // Link the progression callback
        if (progress) {
            xhr.addEventListener('progress', (dataEvent) => {
                progress(dataEvent.loaded, dataEvent.total);
            });
        }
        // open the socket
        xhr.open(method, action, true);
        // configure the header
        if (!isNullOrUndefined(headers)) {
            for (const [key, value] of Object.entries(headers)) {
                xhr.setRequestHeader(key, value as string);
            }
        }
        xhr.send(data);
    });
}

export function RESTRequest({ restModel, restConfig, data, params, queries, progress }: RESTRequestType): Promise<ModelResponseHttp> {
    // Create the URL PATH:
    let generateUrl = RESTUrl({ restModel, restConfig, data, params, queries });
    let headers: any = {};
    if (restConfig.token !== undefined && restModel.tokenInUrl !== true) {
        headers['Authorization'] = `Bearer ${restConfig.token}`;
    }
    if (restModel.accept !== undefined) {
        headers['Accept'] = restModel.accept;
    }
    if (restModel.requestType !== HTTPRequestModel.GET) {
        // if Get we have not a content type, the body is empty
        if (restModel.contentType !== HTTPMimeType.MULTIPART) {
            // special case of multi-part ==> no content type otherwise the browser does not set the ";bundary=--****"
            headers['Content-Type'] = restModel.contentType;
        }
    }
    let body = data;
    if (restModel.contentType === HTTPMimeType.JSON) {
        body = JSON.stringify(data);
    } else if (restModel.contentType === HTTPMimeType.MULTIPART) {
        const formData = new FormData();
        for (const name in data) {
            formData.append(name, data[name]);
        }
        body = formData
    }
    console.log(`Call ${generateUrl}`)
    return new Promise((resolve, reject) => {
        let action: Promise<Response> = undefined;
        if (isNullOrUndefined(progress)) {
            action = fetch(generateUrl, {
                method: restModel.requestType,
                headers,
                body,
            });
        } else {
            action = fetchProgress(generateUrl, {
                method: restModel.requestType,
                headers,
                body,
            }, progress);
        }
        action.then((response: Response) => {
            if (response.status >= 200 && response.status <= 299) {
                const contentType = response.headers.get('Content-Type');
                if (restModel.accept !== contentType) {
                    reject({
                        name: `REST check wrong type: ${restModel.accept} != ${contentType}`,
                        message: "rest-tools.ts Wrong type in the message return type",
                        time: Date().toString(),
                        status: 901,
                        statusMessage: "Fetch error",
                    });
                } else if (contentType === HTTPMimeType.JSON) {
                    response
                        .json()
                        .then((value: any) => {
                            //console.log(`RECEIVE ==> ${response.status}=${ JSON.stringify(value, null, 2)}`);
                            resolve({ status: response.status, data: value });
                        })
                        .catch((reason: any) => {
                            reject({
                                name: `REST parse json fail: ${reason}`,
                                message: "rest-tools.ts Wrong message model to parse",
                                time: Date().toString(),
                                status: 902,
                                statusMessage: "Fetch parse error",
                            });
                        });
                } else {
                    resolve({ status: response.status, data: response.body });
                }
            } else {
                reject({
                    name: `${response.body}`,
                    message: "rest-tools.ts Wrong return code",
                    time: Date().toString(),
                    status: response.status,
                    statusMessage: "Fetch code error",
                });
            }
        }).catch((error: any) => {
            reject({
                name: error,
                message: "http-wrapper.ts detect an error in the fetch request",
                time: Date(),
                status: 999,
                statusMessage: "Fetch catch error"
            });
        });
    });
}



export function RESTRequestJson<TYPE>(request: RESTRequestType, checker: (data: any) => data is TYPE): Promise<TYPE> {
    return new Promise((resolve, reject) => {
        RESTRequest(request).then((value: ModelResponseHttp) => {
            if (checker(value.data)) {
                resolve(value.data);
            } else {
                reject({
                    name: "REST Fail to verify the data",
                    message: "api.ts Check type as fail",
                    time: Date().toString(),
                    status: 950,
                    statusMessage: "API cast ERROR",
                });
            }
        }).catch((reason: RestErrorResponse) => {
            reject(reason);
        });
    });
}
export function RESTRequestJsonArray<TYPE>(request: RESTRequestType, checker: (data: any) => data is TYPE): Promise<TYPE[]> {
    return new Promise((resolve, reject) => {
        RESTRequest(request).then((value: ModelResponseHttp) => {
            if (isArrayOf(value.data, checker)) {
                resolve(value.data);
            } else {
                reject({
                    name: "REST Fail to verify the data",
                    message: "api.ts Check type as fail",
                    time: Date().toString(),
                    status: 950,
                    statusMessage: "API cast ERROR",
                });
            }
        }).catch((reason: RestErrorResponse) => {
            reject(reason);
        });
    });
}

export function RESTRequestVoid(request: RESTRequestType): Promise<void> {
    return new Promise((resolve, reject) => {
        RESTRequest(request).then((value: ModelResponseHttp) => {
            resolve();
        }).catch((reason: RestErrorResponse) => {
            reject(reason);
        });
    });
}
