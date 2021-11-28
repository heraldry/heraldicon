(ns heraldry.frontend.ui.element.fimbriation
  (:require
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.strings :as strings]
   [heraldry.util :as util]
   [re-frame.core :as rf]))

;; TODO: can probably be improved with better subscriptions
(rf/reg-sub :fimbriation-submenu-link-name
  (fn [[_ context] _]
    [(rf/subscribe [:heraldry.state/options context])
     (rf/subscribe [:heraldry.state/sanitized-data context])])

  (fn [[options fimbriation] [_ _context]]
    (let [main-name (case (:mode fimbriation)
                      :none strings/none
                      :single (util/str-tr (-> fimbriation
                                               :tincture-1
                                               tincture/translate-tincture
                                               util/upper-case-first))
                      :double (util/str-tr (-> fimbriation
                                               :tincture-1
                                               tincture/translate-tincture
                                               util/upper-case-first)
                                           " "
                                           strings/and
                                           " "
                                           (-> fimbriation
                                               :tincture-2
                                               tincture/translate-tincture
                                               util/upper-case-first)))
          changes [main-name
                   (when (some #(options/changed? % fimbriation options)
                               [:alignment :thickness-1 :thickness-2])
                     strings/adjusted)]]

      (-> (util/combine ", " changes)
          util/upper-case-first))))

(defn fimbriation-submenu [context]
  (when-let [options (interface/get-relevant-options context)]
    (let [{:keys [ui]} options
          label (:label ui)
          link-name @(rf/subscribe [:fimbriation-submenu-link-name context])]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context label [tr link-name] {:style {:width "22em"}
                                                       :class "submenu-fimbriation"}
         (ui-interface/form-elements
          context
          [:mode
           :alignment
           :corner
           :thickness-1
           :tincture-1
           :thickness-2
           :tincture-2])]]])))

(defmethod ui-interface/form-element :fimbriation [context]
  [fimbriation-submenu context])
