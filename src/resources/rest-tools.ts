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
    // Mode of the TOKEN in URL or Header (?token:${tokenInUrl})
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

// generic progression callback
export type ProgressCallback = (count: number, total: number) => void;

// Rest generic callback have a basic model to upload and download advancement.
export interface RESTCallbacks {
    progressUpload?: ProgressCallback,
    progressDownload?: ProgressCallback
};

export interface RESTRequestType {
    restModel: RESTModel,
    restConfig: RESTConfig,
    data?: any,
    params?: object,
    queries?: object,
    callback?: RESTCallbacks,
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


export function fetchProgress(generateUrl: string, { method, headers, body }: {
    method: HTTPRequestModel,
    headers: any,
    body: any,
}, { progressUpload, progressDownload }: RESTCallbacks): Promise<Response> {
    const xhr = new XMLHttpRequest();
    return new Promise((resolve, reject) => {
        // Stream the upload progress
        if (progressUpload) {
            xhr.upload.addEventListener("progress", (dataEvent) => {
                if (dataEvent.lengthComputable) {
                    //console.log(`    ==> has a progress event: ${dataEvent.loaded} / ${dataEvent.total}`);
                    progressUpload(dataEvent.loaded, dataEvent.total);
                }
            });
        }
        // Stream the download progress
        if (progressDownload) {
            xhr.addEventListener("progress", (dataEvent) => {
                if (dataEvent.lengthComputable) {
                    //console.log(`    ==> download progress:: ${dataEvent.loaded} / ${dataEvent.total}`);
                    progressUpload(dataEvent.loaded, dataEvent.total);
                }
            });
        }
        // Check if we have an internal Fail:
        xhr.addEventListener('error', () => {
            reject(new TypeError('Failed to fetch'))
        });
        // Capture the end of the stream
        xhr.addEventListener("loadend", () => {
            if (xhr.readyState != 4) {
                console.log(`    ==> READY state`);
                return;
            }
            // Stream is ended, transform in a generic response:
            const response = new Response(xhr.response, {
                status: xhr.status,
                statusText: xhr.statusText
            });
            const headersArray = xhr.getAllResponseHeaders().trim().replaceAll("\r\n", "\n").split('\n');
            headersArray.forEach(function (header) {
                const firstColonIndex = header.indexOf(':');
                if (firstColonIndex !== -1) {
                    var key = header.substring(0, firstColonIndex).trim();
                    var value = header.substring(firstColonIndex + 1).trim();
                    response.headers.set(key, value);
                } else {
                    response.headers.set(header, "");
                }
            });
            resolve(response);
        });
        xhr.open(method, generateUrl, true);
        if (!isNullOrUndefined(headers)) {
            for (const [key, value] of Object.entries(headers)) {
                xhr.setRequestHeader(key, value as string);
            }
        }
        console.log(`    ==> send`);
        xhr.send(body);
        console.log(`    ==> send done`);
    });
}

export function RESTRequest({ restModel, restConfig, data, params, queries, callback }: RESTRequestType): Promise<ModelResponseHttp> {
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
    return new Promise((resolve, reject) => {
        let action: undefined | Promise<Response> = undefined;
        if (isNullOrUndefined(callback) || (isNullOrUndefined(callback.progressDownload) && isNullOrUndefined(callback.progressUpload))) {
            // No information needed: call the generic fetch interface
            action = fetch(generateUrl, {
                method: restModel.requestType,
                headers,
                body,
            });
        } else {
            // need progression information: call old fetch model (XMLHttpRequest) that permit to keep % upload and % download for HTTP1.x
            action = fetchProgress(generateUrl, {
                method: restModel.requestType ?? HTTPRequestModel.GET,
                headers,
                body,
            }, callback);
        }
        action.then((response: Response) => {
            if (response.status >= 200 && response.status <= 299) {
                const contentType = response.headers.get('Content-Type');
                if (restModel.accept !== contentType) {
                    reject({
                        time: Date().toString(),
                        status: 901,
                        error: `REST check wrong type: ${restModel.accept} != ${contentType}`,
                        statusMessage: "Fetch error",
                        message: "rest-tools.ts Wrong type in the message return type"
                    } as RestErrorResponse);
                } else if (contentType === HTTPMimeType.JSON) {
                    response
                        .json()
                        .then((value: any) => {
                            //console.log(`RECEIVE ==> ${response.status}=${ JSON.stringify(value, null, 2)}`);
                            resolve({ status: response.status, data: value });
                        })
                        .catch((reason: any) => {
                            reject({
                                time: Date().toString(),
                                status: 902,
                                error: `REST parse json fail: ${reason}`,
                                statusMessage: "Fetch parse error",
                                message: "rest-tools.ts Wrong message model to parse"
                            } as RestErrorResponse);
                        });
                } else {
                    resolve({ status: response.status, data: response.body });
                }
            } else {
                reject({
                    time: Date().toString(),
                    status: response.status,
                    error: `${response.body}`,
                    statusMessage: "Fetch code error",
                    message: "rest-tools.ts Wrong return code"
                } as RestErrorResponse);
            }
        }).catch((error: any) => {
            reject({
                time: Date(),
                status: 999,
                error: error,
                statusMessage: "Fetch catch error",
                message: "http-wrapper.ts detect an error in the fetch request"
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
                    time: Date().toString(),
                    status: 950,
                    error: "REST Fail to verify the data",
                    statusMessage: "API cast ERROR",
                    message: "api.ts Check type as fail"
                } as RestErrorResponse);
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
                    time: Date().toString(),
                    status: 950,
                    error: "REST Fail to verify the data",
                    statusMessage: "API cast ERROR",
                    message: "api.ts Check type as fail"
                } as RestErrorResponse);
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
