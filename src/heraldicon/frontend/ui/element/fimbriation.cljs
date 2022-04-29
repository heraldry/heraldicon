(ns heraldicon.frontend.ui.element.fimbriation
  (:require
   [heraldicon.coat-of-arms.tincture.core :as tincture]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.ui.element.submenu :as submenu]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]
   [heraldicon.util :as util]))

;; TODO: can probably be improved with better subscriptions
(defn submenu-link-name [options fimbriation]
  (let [main-name (case (:mode fimbriation)
                    :none :string.option.type-fimbriation-choice/none
                    :single (util/str-tr (-> fimbriation
                                             :tincture-1
                                             tincture/translate-tincture))
                    :double (util/str-tr (-> fimbriation
                                             :tincture-1
                                             tincture/translate-tincture)
                                         " "
                                         :string.miscellaneous/and
                                         " "
                                         (-> fimbriation
                                             :tincture-2
                                             tincture/translate-tincture)))
        changes [main-name
                 (when (some #(options/changed? % fimbriation options)
                             [:alignment :thickness-1 :thickness-2])
                   :string.submenu-summary/adjusted)]]

    (-> (util/combine ", " changes)
        util/upper-case-first)))

(defn fimbriation-submenu [context]
  (when-let [options (interface/get-relevant-options context)]
    (let [{:keys [ui]} options
          label (:label ui)
          link-name (submenu-link-name options (interface/get-sanitized-data context))]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context label [tr link-name] {:style {:width "22em"}
                                                       :class "submenu-fimbriation"}
         (ui.interface/form-elements
          context
          [:mode
           :alignment
           :corner
           :thickness-1
           :tincture-1
           :thickness-2
           :tincture-2])]]])))

(defmethod ui.interface/form-element :fimbriation [context]
  [fimbriation-submenu context])
