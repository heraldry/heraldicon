(ns heraldicon.frontend.element.semy-layout
  (:require
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.options :as options]
   [re-frame.core :as rf]))

;; TODO: probably can be improved with better subscriptions
(rf/reg-sub ::link-name
  (fn [[_ context] _]
    [(rf/subscribe [:get context])
     (rf/subscribe [::interface/options (:path context)])])

  (fn [[layout options] [_ _path]]
    (let [sanitized-layout (options/sanitize layout options)
          main-name (string/str-tr (:num-fields-x sanitized-layout) "x"
                                   (:num-fields-y sanitized-layout)
                                   " "
                                   :string.miscellaneous/fields)
          changes [main-name
                   (when (some #(options/changed? % sanitized-layout options)
                               [:offset-x :offset-y])
                     :string.submenu-summary/shifted)
                   (when (some #(options/changed? % sanitized-layout options)
                               [:stretch-x :stretch-y])
                     :string.submenu-summary/stretched)
                   (when (options/changed? :rotation sanitized-layout options)
                     :string.submenu-summary/rotated)]]
      (string/upper-case-first (string/combine ", " changes)))))

(defmethod element/element :ui.element/semy-layout [context]
  (when-let [options (interface/get-relevant-options context)]
    (let [{:ui/keys [label]} options
          link-name @(rf/subscribe [::link-name context])]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context label [tr link-name] {:style {:width "22em"}
                                                       :class "submenu-semy-layout"}
         (element/elements
          context
          [:num-fields-x
           :num-fields-y
           :offset-x
           :offset-y
           :stretch-x
           :stretch-y
           :rotation])]]])))
