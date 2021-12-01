(ns heraldry.frontend.ui.element.value-mode-select
  (:require
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.element.hover-menu :as hover-menu]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(defn value-mode-select [context & {:keys [display-fn disabled? on-change default-option]}]
  (let [current-value (interface/get-raw-data context)
        handler-for-value (fn [new-value]
                            (fn [event]
                              (if on-change
                                (do
                                  (on-change new-value)
                                  (.stopPropagation event))
                                (state/dispatch-on-event event [:set context new-value]))))
        {:keys [inherited
                default
                type
                choices
                ui]} (or (interface/get-relevant-options context)
                         default-option)
        {:keys [additional-values]} ui
        display-fn (or display-fn
                       (when (= type :choice)
                         (fn [v]
                           ((util/choices->map choices) v)))
                       identity)
        effective-value (->> [current-value
                              inherited
                              default]
                             (keep (fn [v]
                                     (when-not (nil? v)
                                       v)))
                             first)
        menu (cond-> []
               (and (some? inherited)
                    (some? default)) (conj {:title (util/str-tr (string "Default") " (" (display-fn default) ")")
                                            :icon "fas fa-redo"
                                            :handler (handler-for-value default)})
               (or (some? inherited)
                   (some? default)) (conj {:title (util/str-tr (if (some? inherited)
                                                                 (string "Inherited")
                                                                 (string "Auto"))
                                                               " (" (display-fn (or inherited default)) ")")
                                           :icon (if (some? current-value)
                                                   "far fa-square"
                                                   "far fa-check-square")
                                           :handler (handler-for-value nil)})
               (seq additional-values) (-> (concat (map (fn [[display-value value]]
                                                          {:title (util/str-tr display-value " (" (display-fn value) ")")
                                                           :icon (if (= current-value value)
                                                                   "far fa-check-square"
                                                                   "far fa-square")
                                                           :handler (handler-for-value value)})
                                                        additional-values))
                                           vec))
        manual-icon (if (and (some? current-value)
                             (not (seq (filter (fn [[_ value]]
                                                 (= current-value value)) additional-values))))
                      "far fa-check-square"
                      "far fa-square")
        menu (cond-> menu
               (seq menu) (conj {:title [tr (string "Manual")]
                                 :icon manual-icon
                                 :handler (handler-for-value effective-value)}))]

    [:<>
     (when (seq menu)
       [:div {:style {:display "inline-block"
                      :margin-left "0.5em"
                      :position "absolute"}}
        [hover-menu/hover-menu
         context
         strings/mode
         menu
         [:i.ui-icon {:class "fas fa-cog"}]
         :disabled? disabled?]])]))
