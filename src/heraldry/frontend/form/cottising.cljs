(ns heraldry.frontend.form.cottising
  (:require [heraldry.coat-of-arms.options :as options]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.line :as line]
            [heraldry.frontend.form.state]
            [heraldry.frontend.form.tincture :as tincture]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

(defn form-for-single-cottise [path options & {:keys [title] :or {title "Cottise"}}]
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
         :label "Tincture"])]]))

(defn form [path options & {:keys [title] :or {title "Cottising"}}]
  (let [{:keys [mode]} @(rf/subscribe [:get path])
        link-name      (case mode
                         :single (util/combine
                                  ", "
                                  ["single"])
                         :double (util/combine
                                  ", "
                                  ["double"])
                         "None")]
    [:div.setting
     [:label title]
     " "
     [element/submenu path "Cottising" link-name {}
      (when (-> options :mode)
        [element/radio-select (conj path :mode) (-> options :mode :choices)
         :default (-> options :mode :default)])
      (when (#{:single :double} mode)
        [:<>
         [form-for-single-cottise (conj path :cottise-1) (:cottise-1 options)
          :title (str "Cottise" (when (= mode :double) " 1"))]
         [form-for-single-cottise (conj path :cottise-opposite-1) (:cottise-opposite-1 options)
          :title (str "Cottise" (when (= mode :double) " 1") " Opp.")]
         (when (= :double mode)
           [:<>
            [form-for-single-cottise (conj path :cottise-2) (:cottise-2 options)
             :title (str "Cottise" (when (= mode :double) " 2"))]
            [form-for-single-cottise (conj path :cottise-opposite-2) (:cottise-opposite-2 options)
             :title (str "Cottise" (when (= mode :double) " 2") " Opp.")]])])]]))

