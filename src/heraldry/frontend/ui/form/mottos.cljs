(ns heraldry.frontend.ui.form.mottos
  (:require [heraldry.coat-of-arms.default :as default]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.interface :as ui-interface]
            [heraldry.frontend.ui.shield-separator :as shield-separator]
            [heraldry.strings :as strings]
            [re-frame.core :as rf]))

(defn form [path _]
  [:<>
   (for [option []]
     ^{:key option} [ui-interface/form-element (conj path option)])])

(defmethod ui-interface/component-node-data :heraldry.component/mottos [path]
  (let [mottos-path (conj path :elements)
        num-mottos @(rf/subscribe [:get-list-size mottos-path])]
    {:title {:en "Mottos and slogans"
             :de "Wahlsprüche und Slogans"}
     :annotation [:div.tooltip.info {:style {:display "inline-block"
                                             :margin-left "0.2em"}}
                  [:sup {:style {:color "#d40"}}
                   "alpha"]
                  [:div.bottom
                   [:p strings/alpha-feature]]]
     :buttons [{:icon "fas fa-plus"
                :title strings/add
                :menu [{:title strings/motto
                        :handler #(state/dispatch-on-event % [:add-element mottos-path default/motto
                                                              shield-separator/add-element-options])}
                       {:title strings/slogan
                        :handler #(state/dispatch-on-event % [:add-element mottos-path default/slogan
                                                              shield-separator/add-element-options])}]}]
     :nodes (->> (range num-mottos)
                 reverse
                 (map (fn [idx]
                        (let [motto-path (conj mottos-path idx)
                              removable? @(rf/subscribe [:element-removable? motto-path])]
                          {:path motto-path
                           :buttons (cond-> [{:icon "fas fa-chevron-down"
                                              :disabled? (zero? idx)
                                              :tooltip strings/move-down
                                              :handler #(state/dispatch-on-event % [:move-element motto-path (dec idx)])}
                                             {:icon "fas fa-chevron-up"
                                              :disabled? (= idx (dec num-mottos))
                                              :tooltip strings/move-up
                                              :handler #(state/dispatch-on-event % [:move-element motto-path (inc idx)])}]
                                      removable? (conj {:icon "far fa-trash-alt"
                                                        :tooltip strings/remove
                                                        :handler #(state/dispatch-on-event
                                                                   %
                                                                   [:remove-element motto-path
                                                                    shield-separator/remove-element-options])}))}))))}))

(defmethod ui-interface/component-form-data :heraldry.component/mottos [_path]
  {:form form})
