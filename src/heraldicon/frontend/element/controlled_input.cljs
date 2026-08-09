(ns heraldicon.frontend.element.controlled-input
  (:require
   [reagent.core :as r]))

(defn input
  "Controlled text input that is robust against mobile IME / predictive-text
  (\"writing assist\") composition.

  A plain controlled input re-drives the DOM node's `value` from external state
  on every keystroke. Doing that while a composition is active (predictive text,
  autocorrect, glide typing, IME) disrupts the composition buffer and duplicates
  the composed text. To avoid that, this component:

  - tracks composition and never propagates changes to `:on-change` while a
    composition is in progress (only on composition end and on blur), and
  - drives the displayed value from local state that mirrors exactly what the
    user typed while the field is focused, so the DOM value is never reset from
    external state mid-edit.

  `attrs` are ordinary `:input` attributes. `:on-change` receives the new string
  value (not the DOM event). `:value` is the externally controlled value, shown
  while the field is not being edited."
  [_attrs]
  (let [;; the in-progress value while the field is focused; nil means the field
        ;; is not being edited and the external :value is shown verbatim
        local (r/atom nil)
        composing? (r/atom false)]
    (fn [{:keys [value on-change on-focus on-blur] :as attrs}]
      (let [editing? (some? @local)
            commit (fn [new-value]
                     (when on-change
                       (on-change new-value)))]
        [:input
         (assoc attrs
                :value (if editing? @local (or value ""))
                :on-focus (fn [event]
                            (reset! local (or value ""))
                            (when on-focus
                              (on-focus event)))
                :on-change (fn [event]
                             (let [new-value (-> event .-target .-value)]
                               (reset! local new-value)
                               (when-not @composing?
                                 (commit new-value))))
                :on-composition-start (fn [_event]
                                        (reset! composing? true))
                :on-composition-end (fn [event]
                                      (reset! composing? false)
                                      (let [new-value (-> event .-target .-value)]
                                        (reset! local new-value)
                                        (commit new-value)))
                :on-blur (fn [event]
                           (let [new-value (or @local value "")]
                             (reset! composing? false)
                             (reset! local nil)
                             (commit new-value)
                             (when on-blur
                               (on-blur event)))))]))))
