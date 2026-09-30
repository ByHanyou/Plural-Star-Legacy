import React, {forwardRef, ForwardedRef, ReactElement, Ref} from 'react';
import {FlashList as ListV1} from 'flash-list-v1';
import type {FlashListProps as ListV1Props} from 'flash-list-v1';

export * from 'flash-list-v1';

export type FlashListRef<T> = ListV1<T>;

type VisiblePosition = {
  disabled?: boolean;
  minIndexForVisible?: number;
  autoscrollToTopThreshold?: number;
};

export type FlashListProps<T> = Omit<ListV1Props<T>, 'maintainVisibleContentPosition'> & {
  maintainVisibleContentPosition?: VisiblePosition;
};

function Inner<T>({maintainVisibleContentPosition: mvcp, ...rest}: FlashListProps<T>, ref: ForwardedRef<ListV1<T>>) {
  const keep = mvcp && !mvcp.disabled && mvcp.minIndexForVisible !== undefined
    ? {minIndexForVisible: mvcp.minIndexForVisible, autoscrollToTopThreshold: mvcp.autoscrollToTopThreshold}
    : undefined;
  return <ListV1<T> ref={ref} {...rest} {...(keep ? {maintainVisibleContentPosition: keep} : {})} />;
}

export const FlashList = forwardRef(Inner) as <T>(props: FlashListProps<T> & {ref?: Ref<ListV1<T>>}) => ReactElement | null;
