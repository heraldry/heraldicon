(ns heraldry.frontend.form.cottise
  (:require [heraldry.coat-of-arms.options :as options]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.line :as line]
            [heraldry.frontend.form.state]
            [heraldry.frontend.form.tincture :as tincture]
            [re-frame.core :as rf]))

(defn form [path options & {:keys [title] :or {title "Cottise"}}]
  (let [cottise @(rf/subscribe [:get path])]
    [:div.setting
     [:label title]
     " "
     [element/submenu path title "Cottise" {}
      (when (:line options)
        [line/form (conj path :line) :options (:line options)])
      (when (:opposite-line options)
        [line/form (conj path :opposite-line)
         :options (:opposite-line options)
         :defaults (options/sanitize (:line cottise) (:line options))
         :title "Opposite Line"])
      (when (-> options :distance)
        [element/range-input (conj path :distance)
         "Distance"
         (-> options :distance :min)
         (-> options :distance :max)
         :default (-> options :distance :default)
         :step 0.1
         :display-function #(str % "%")])
      (when (-> options :thickness)
        [element/range-input (conj path :thickness)
         "Thickness"
         (-> options :thickness :min)
         (-> options :thickness :max)
         :default (-> options :thickness :default)
         :step 0.1
         :display-function #(str % "%")])
      (when (-> options :tincture)
        [tincture/form (conj path :tincture)
         :label "Tincture"])
      (when (:cottise options)
        [form (conj path :cottise) (:cottise options)])]]))

