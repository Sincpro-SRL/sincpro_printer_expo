// Reexport the native module. On web, it will be resolved to ExpoBixolonModule.web.ts
// and on native platforms to ExpoBixolonModule.ts
export { default } from './ExpoBixolonModule';
export { default as ExpoBixolonView } from './ExpoBixolonView';
export * from  './ExpoBixolon.types';
