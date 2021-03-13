(ns heraldry.frontend.form.position
  (:require [clojure.string :as s]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.frontend.form.element :as element]
            [re-frame.core :as rf]))

(defn form [path & {:keys [title options] :or {title "Position"}}]
  (let [position @(rf/subscribe [:get path])
        point-path (conj path :point)
        alignment-path (conj path :alignment)
        angle-path (conj path :angle)
        offset-x-path (conj path :offset-x)
        offset-y-path (conj path :offset-y)
        current-point (options/get-value (:point position) (:point options))
        current-alignment (options/get-value (:alignment position) (:alignment options))
        current-offset-x (options/get-value (:offset-x position) (:offset-x options))
        current-offset-y (options/get-value (:offset-y position) (:offset-y options))]
    [:div.setting
     [:label title]
     " "
     [element/submenu path title (if (= current-point :angle)
                                   (position/anchor-point-map current-point)
                                   (str
                                    (position/anchor-point-map current-point)
                                    (when (or (-> current-offset-x (or 0) zero? not)
                                              (-> current-offset-y (or 0) zero? not))
                                      " (adjusted)")
                                    (when (-> current-alignment (or :middle) (not= :middle))
                                      (str ", " (s/lower-case (position/alignment-map current-alignment)))))) {}
      [element/select point-path "Point" (-> options :point :choices)
       :on-change #(do
                     (rf/dispatch [:set point-path %])
                     (rf/dispatch [:set angle-path nil])
                     (rf/dispatch [:set offset-x-path nil])
                     (rf/dispatch [:set offset-y-path nil]))
       :default (-> options :point :default)]
      (when (-> options :alignment)
        [element/select alignment-path "Alignment" (-> options :alignment :choices)])
      (when (-> options :angle)
        [element/range-input-with-checkbox angle-path "Angle"
         (-> options :angle :min)
         (-> options :angle :max)
         :step 1
         :default (options/get-value (:angle position) (:angle options))])
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
