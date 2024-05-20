/** @file
 * @author Edouard DUPIN
 * @copyright 2024, Edouard DUPIN, all right reserved
 * @license MPL-2
 */

import { z as zod, ZodTypeAny, ZodObject } from 'zod';

export function removeReadonly<T extends ZodTypeAny>(schema: T): T {
  if (schema instanceof ZodObject) {
    const shape: Record<string, ZodTypeAny> = {};
    for (const key in schema.shape) {
      const field = schema.shape[key];
      shape[key] = field._def.typeName === 'ZodReadonly'
        ? field._def.innerType
        : removeReadonly(field);
    }
    return zod.object(shape) as T;
  }
  return schema;
}