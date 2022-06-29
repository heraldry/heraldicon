(ns heraldicon.frontend.component.motto
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.ribbon :as ribbon]
   [heraldicon.frontend.interface :as ui.interface]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.util.core :as util]
   [re-frame.core :as rf]))

(rf/reg-sub :motto-name
  (fn [[_ path] _]
    (rf/subscribe [:get (drop-last path)]))

  (fn [elements [_ path]]
    ;; TODO: fix numbering/naming
    (let [idx (last path)
          mottos (keep-indexed (fn [idx element]
                                 (when (-> element :type (= :heraldry.motto.type/motto))
                                   idx)) elements)
          slogans (keep-indexed (fn [idx element]
                                  (when (-> element :type (= :heraldry.motto.type/slogan))
                                    idx)) elements)
          relevant-elements (if (some (set [idx]) mottos)
                              mottos
                              slogans)]
      (string/str-tr
       (when (-> relevant-elements count (> 1))
         (str (inc (util/index-of idx relevant-elements)) ". "))
       (if (= relevant-elements mottos)
         :string.entity/motto
         :string.entity/slogan)))))

(defn- form [context]
  [:<>
   (ui.interface/form-elements
    context
    [:type
     :anchor
     :geometry])

   [:div {:style {:font-size "1.3em"
                  :margin-top "0.5em"
                  :margin-bottom "0.5em"}} [tr :string.entity/tincture]]

   (ui.interface/form-elements
    context
    [:tincture-foreground
     :tincture-background
     :tincture-text])

   [:div {:style {:font-size "1.3em"
                  :margin-top "0.5em"
                  :margin-bottom "0.5em"}} [tr :string.entity/ribbon]]

   [ui.interface/form-element (c/++ context :ribbon-variant)]

   (when (interface/get-raw-data (c/++ context :ribbon-variant))
     (let [ribbon-context (c/++ context :ribbon)]
       [:<>
        [ribbon/form ribbon-context]
        [ribbon/segments-form ribbon-context]]))])

(defmethod ui.interface/component-node-data :heraldry/motto [{:keys [path]}]
  {:title @(rf/subscribe [:motto-name path])})

(defmethod ui.interface/component-form-data :heraldry/motto [_context]
  {:form form})