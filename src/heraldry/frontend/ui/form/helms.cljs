(ns heraldry.frontend.ui.form.helms
  (:require
   [heraldry.coat-of-arms.default :as default]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.strings :as strings]
   [re-frame.core :as rf]))

(defn form [_context]
  [:<>])

(defmethod ui-interface/component-node-data :heraldry.component/helms [{:keys [path] :as context}]
  (let [helms-path (conj path :elements)
        num-helms @(rf/subscribe [:get-list-size helms-path])]
    {:title {:en "Helms and crests"
             :de "Helme und Helmzier"}
     :annotation [:div.tooltip.info {:style {:display "inline-block"
                                             :margin-left "0.2em"}}
                  [:sup {:style {:color "#d40"}}
                   "alpha"]
                  [:div.bottom
                   [:p strings/alpha-feature]]]
     :buttons [{:icon "fas fa-plus"
                :handler #(state/dispatch-on-event % [:add-element helms-path default/helm])}]
     :nodes (->> (range num-helms)
                 (map (fn [idx]
                        (let [helm-path (conj helms-path idx)]
                          {:context (assoc context :path helm-path)
                           :buttons [{:icon "fas fa-chevron-up"
                                      :disabled? (zero? idx)
                                      :tooltip strings/move-down
                                      :handler #(state/dispatch-on-event % [:move-element helm-path (dec idx)])}
                                     {:icon "fas fa-chevron-down"
                                      :disabled? (= idx (dec num-helms))
                                      :tooltip strings/move-up
                                      :handler #(state/dispatch-on-event % [:move-element helm-path (inc idx)])}
                                     {:icon "far fa-trash-alt"
                                      :tooltip strings/remove
                                      :handler #(state/dispatch-on-event % [:remove-element helm-path])}]}))))}))

(defmethod ui-interface/component-form-data :heraldry.component/helms [_context]
  {:form form})
