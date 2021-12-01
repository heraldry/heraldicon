(ns heraldry.frontend.ui.form.motto
  (:require
   [heraldry.context :as c]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.ui.form.ribbon-general :as ribbon-general]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.util :as util]
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
      (util/str-tr
       (when (-> relevant-elements count (> 1))
         (str (inc (util/index-of idx relevant-elements)) ". "))
       (if (= relevant-elements mottos)
         (string "Motto")
         (string "Slogan"))))))

(defn form [context]
  [:<>
   (ui-interface/form-elements
    context
    [:type
     :origin
     :geometry])

   [:div {:style {:font-size "1.3em"
                  :margin-top "0.5em"
                  :margin-bottom "0.5em"}} [tr (string "Tincture")]]

   (ui-interface/form-elements
    context
    [:tincture-foreground
     :tincture-background
     :tincture-text])

   [:div {:style {:font-size "1.3em"
                  :margin-top "0.5em"
                  :margin-bottom "0.5em"}} [tr (string "Ribbon")]]

   [ui-interface/form-element (c/++ context :ribbon-variant)]

   (when (interface/get-raw-data (c/++ context :ribbon-variant))
     (let [ribbon-context (c/++ context :ribbon)]
       [:<>
        [ribbon-general/ribbon-form ribbon-context]
        [ribbon-general/ribbon-segments-form ribbon-context]]))])

(defmethod ui-interface/component-node-data :heraldry.component/motto [{:keys [path]}]
  {:title @(rf/subscribe [:motto-name path])})

(defmethod ui-interface/component-form-data :heraldry.component/motto [_context]
  {:form form})
