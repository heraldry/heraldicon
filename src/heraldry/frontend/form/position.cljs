(ns heraldry.frontend.form.position
  (:require [heraldry.coat-of-arms.options :as options]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

(defn form [path & {:keys [title options] :or {title "Position"}}]
  (let [position @(rf/subscribe [:get path])
        point-path (conj path :point)
        offset-x-path (conj path :offset-x)
        offset-y-path (conj path :offset-y)]
    [:div.setting
     [:label title]
     " "
     [element/submenu path title (str (-> position
                                          :point
                                          (or :fess)
                                          (util/translate-cap-first))
                                      " point" (when (or (-> position :offset-x (or 0) zero? not)
                                                         (-> position :offset-y (or 0) zero? not))
                                                 " (adjusted)")) {}
      [element/select point-path "Point" (-> options :point :choices)
       :on-change #(do
                     (rf/dispatch [:set point-path %])
                     (rf/dispatch [:set offset-x-path nil])
                     (rf/dispatch [:set offset-y-path nil]))]
      (when (:offset-x options)
        [element/range-input-with-checkbox offset-x-path "Offset x"
         (-> options :offset-x :min)
         (-> options :offset-x :max)
         :default (options/get-value (:offset-x position) (:offset-x options))
         :display-function #(str % "%")])
      (when (:offset-y options)
        [element/range-input-with-checkbox offset-y-path "Offset y"
         (-> options :offset-y :min)
         (-> options :offset-y :max)
         :default (options/get-value (:offset-y position) (:offset-y options))
         :display-function #(str % "%")])]]))
