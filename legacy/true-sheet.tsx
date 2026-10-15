import React, {Component, createRef, ReactNode} from 'react';
import {ColorValue, Dimensions, StyleSheet, View} from 'react-native';
import {TrueSheet as SheetV2} from 'true-sheet-v2';

type Detent = number | 'auto';

export interface TrueSheetProps {
  detents?: Detent[];
  cornerRadius?: number;
  backgroundColor?: ColorValue;
  dismissible?: boolean;
  grabber?: boolean;
  scrollable?: boolean;
  header?: ReactNode;
  children?: ReactNode;
  onDidPresent?: () => void;
  onDidDismiss?: () => void;
}

const toSize = (d: Detent) => (d === 'auto' ? 'auto' : (`${Math.round(d * 100)}%` as `${number}%`));

let pendingDismiss: Promise<void> | null = null;
const DISMISS_SETTLE_MS = 1500;

export class TrueSheet extends Component<TrueSheetProps> {
  private sheet = createRef<SheetV2>();
  private settleDismiss: (() => void) | null = null;

  present(index?: number): Promise<void> {
    const sheet = this.sheet.current;
    if (!sheet) return Promise.resolve();
    const wait = pendingDismiss || Promise.resolve();
    return wait.then(() => sheet.present(index));
  }

  resize(index: number): Promise<void> {
    return this.sheet.current ? this.sheet.current.resize(index) : Promise.resolve();
  }

  dismiss(): Promise<void> {
    const sheet = this.sheet.current;
    if (!sheet) return Promise.resolve();
    let done: () => void = () => {};
    const p = new Promise<void>(resolve => { done = resolve; });
    const settle = () => {
      if (pendingDismiss === p) pendingDismiss = null;
      if (this.settleDismiss === settle) this.settleDismiss = null;
      done();
    };
    this.settleDismiss = settle;
    pendingDismiss = p;
    setTimeout(settle, DISMISS_SETTLE_MS);
    return sheet.dismiss();
  }

  componentWillUnmount() {
    this.settleDismiss?.();
  }

  render() {
    const {detents = ['auto'], cornerRadius, backgroundColor, dismissible, grabber, header, children, onDidPresent, onDidDismiss} = this.props;
    const tallest = Math.max(0, ...detents.map(d => (typeof d === 'number' ? d : 0)));
    const floor = tallest > 0 ? {minHeight: Math.round(Dimensions.get('window').height * tallest)} : null;
    return (
      <SheetV2
        ref={this.sheet}
        sizes={detents.map(toSize)}
        cornerRadius={cornerRadius}
        backgroundColor={backgroundColor}
        dismissible={dismissible}
        grabber={grabber}
        edgeToEdge
        contentContainerStyle={s.fill}
        onPresent={() => onDidPresent?.()}
        onDismiss={() => { this.settleDismiss?.(); onDidDismiss?.(); }}>
        <View style={[s.fill, floor]}>
          {header}
          {children}
        </View>
      </SheetV2>
    );
  }
}

const s = StyleSheet.create({fill: {flex: 1}});
