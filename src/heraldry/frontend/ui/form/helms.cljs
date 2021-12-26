(ns heraldry.frontend.ui.form.helms
  (:require
   [heraldry.coat-of-arms.default :as default]
   [heraldry.context :as c]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]))

(defn form [_context]
  [:<>])

(defmethod ui-interface/component-node-data :heraldry.component/helms [context]
  (let [elements-context (c/++ context :elements)
        num-helms (interface/get-list-size elements-context)]
    {:title (string "Helms and crests")
     :annotation [:div.tooltip.info {:style {:display "inline-block"
                                             :margin-left "0.2em"}}
                  [:sup {:style {:color "#d40"}}
                   "alpha"]
                  [:div.bottom
                   [:p (string "This feature is incomplete and likely going to change, so use with caution. :)")]]]
     :buttons [{:icon "fas fa-plus"
                :handler #(state/dispatch-on-event % [:add-element elements-context default/helm])}]
     :nodes (->> (range num-helms)
                 (map (fn [idx]
                        (let [helm-context (c/++ elements-context idx)]
                          {:context helm-context
                           :buttons [{:icon "fas fa-chevron-up"
                                      :disabled? (zero? idx)
                                      :tooltip (string "move down")
                                      :handler #(state/dispatch-on-event % [:move-element helm-context (dec idx)])}
                                     {:icon "fas fa-chevron-down"
                                      :disabled? (= idx (dec num-helms))
                                      :tooltip (string "move up")
                                      :handler #(state/dispatch-on-event % [:move-element helm-context (inc idx)])}
                                     {:icon "far fa-trash-alt"
                                      :remove? true
                                      :tooltip (string "remove")
                                      :handler #(state/dispatch-on-event % [:remove-element helm-context])}]}))))}))

(defmethod ui-interface/component-form-data :heraldry.component/helms [_context]
  {:form form})
