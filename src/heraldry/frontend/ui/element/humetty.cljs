(ns heraldry.frontend.ui.element.humetty
  (:require
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.strings :as strings]
   [heraldry.util :as util]
   [re-frame.core :as rf]))

;; TODO: probably can be improved with better subscriptions
(rf/reg-sub :humetty-submenu-link-name
  (fn [[_ context] _]
    [(rf/subscribe [:heraldry.state/options context])
     (rf/subscribe [:heraldry.state/sanitized-data context])])

  (fn [[options humetty] [_ _path]]
    (let [changes (concat
                   (when (:humetty? humetty)
                     [strings/humetty])
                   (when (and (:humetty? humetty)
                              (some #(options/changed? % humetty options)
                                    [:distance]))
                     [strings/resized]))]
      (if (seq changes)
        (-> (util/combine ", " changes)
            util/upper-case-first)
        strings/no))))

(defn humetty-submenu [context]
  (when-let [options (interface/get-relevant-options context)]
    (let [{:keys [ui]} options
          label (:label ui)
          link-name @(rf/subscribe [:humetty-submenu-link-name context])]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context label link-name {:style {:width "22em"}
                                                  :class "submenu-humetty"}
         (ui-interface/form-elements
          context
          [:humetty?
           :corner
           :distance])]]])))

(defmethod ui-interface/form-element :humetty [context]
  [humetty-submenu context])
