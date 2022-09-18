(ns heraldicon.heraldry.ordinary.auto-arrange
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.ordinary.post-process :as post-process]
   [heraldicon.interface :as interface]
   [heraldicon.math.core :as math]))

(defn- margin-factor [ordinary-type]
  (case ordinary-type
    0.3333333333))

(defn size [ordinary-type num-ordinaries]
  (let [[base-size min-size factor] (case ordinary-type
                                      [25 7 0.85])]
    (max (* base-size (js/Math.pow factor (dec num-ordinaries)))
         min-size)))

(defn- margin [ordinary-type size]
  (* (margin-factor ordinary-type) size))

(defn- get-auto-positioned-ordinaries [context ordinary-type]
  (let [num-elements (interface/get-list-size context)]
    (into []
          (comp
           (map #(c/++ context %))
           (filter (fn [component-context]
                     (and (= (interface/get-raw-data (c/++ component-context :type))
                             ordinary-type)
                          (= (or (interface/get-raw-data (c/++ component-context :anchor :point))
                                 :auto)
                             :auto)))))
          (range num-elements))))

(defmethod interface/auto-ordinary-info :default [ordinary-type context]
  (let [ordinaries (get-auto-positioned-ordinaries (c/++ context :components) ordinary-type)
        num-ordinaries (count ordinaries)
        default-size (size ordinary-type num-ordinaries)]
    {:ordinary-contexts ordinaries
     :num-ordinaries num-ordinaries
     :affected-paths (if (> num-ordinaries 1)
                       (into {}
                             (map-indexed (fn [index {:keys [path]}]
                                            [path index]))
                             ordinaries)
                       {})
     :default-size default-size
     :margin (margin ordinary-type default-size)}))

(defn set-offset-x [{:keys [context percentage-base]
                     :as ordinary}]
  (assoc ordinary
         :offset-x (math/percent-of percentage-base
                                    (interface/get-sanitized-data (c/++ context :anchor :offset-x)))))

(defn set-offset-y [{:keys [context percentage-base]
                     :as ordinary}]
  (assoc ordinary
         :offset-y (math/percent-of percentage-base
                                    (interface/get-sanitized-data (c/++ context :anchor :offset-y)))))

(defn set-size [{:keys [context]
                 :as ordinary}]
  (update ordinary
          :size (fn [size]
                  (or (interface/get-raw-data (c/++ context :geometry :size))
                      size))))

(defn set-line-data [{:keys [context line-length]
                      :as ordinary}]
  (let [{:keys [line
                opposite-line]} (post-process/line-properties {:line-length line-length} context)]
    (assoc ordinary
           :line line
           :opposite-line opposite-line)))

(defn set-cottise-data [{:keys [context line-length percentage-base]
                         :as ordinary}]
  (assoc ordinary
         :cottise-height (+ (cottising/cottise-height (c/++ context :cottising :cottise-1)
                                                      line-length percentage-base)
                            (cottising/cottise-height (c/++ context :cottising :cottise-2)
                                                      line-length percentage-base))
         :opposite-cottise-height (+ (cottising/cottise-height (c/++ context :cottising :cottise-opposite-1)
                                                               line-length percentage-base)
                                     (cottising/cottise-height (c/++ context :cottising :cottise-opposite-2)
                                                               line-length percentage-base))))
