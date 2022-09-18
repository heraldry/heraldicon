(ns heraldicon.heraldry.ordinary.auto-arrange
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.ordinary.post-process :as post-process]
   [heraldicon.interface :as interface]
   [heraldicon.math.core :as math]))

(def ^:private margin-factor
  0.3333333333)

(defn size [num-ordinaries]
  (max (* 25 (js/Math.pow 0.85 (dec num-ordinaries)))
       7))

(defn- margin [size]
  (* margin-factor size))

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
        default-size (size num-ordinaries)]
    {:ordinary-contexts ordinaries
     :num-ordinaries num-ordinaries
     :affected-paths (if (> num-ordinaries 1)
                       (into {}
                             (map-indexed (fn [index {:keys [path]}]
                                            [path index]))
                             ordinaries)
                       {})
     :default-size default-size
     :margin (margin default-size)}))

(defn set-offset-x [{:keys [context percentage-base]
                     :as bar}]
  (assoc bar
         :offset-x (math/percent-of percentage-base
                                    (interface/get-sanitized-data (c/++ context :anchor :offset-x)))))

(defn set-offset-y [{:keys [context percentage-base]
                     :as bar}]
  (assoc bar
         :offset-y (math/percent-of percentage-base
                                    (interface/get-sanitized-data (c/++ context :anchor :offset-y)))))

(defn set-size [{:keys [context]
                 :as bar}]
  (update bar
          :size (fn [size]
                  (or (interface/get-raw-data (c/++ context :geometry :size))
                      size))))

(defn set-line-data [{:keys [context line-length]
                      :as bar}]
  (let [{:keys [line
                opposite-line]} (post-process/line-properties {:line-length line-length} context)]
    (assoc bar
           :line line
           :opposite-line opposite-line)))

(defn set-cottise-data [{:keys [context line-length percentage-base]
                         :as bar}]
  (assoc bar
         :cottise-height (+ (cottising/cottise-height (c/++ context :cottising :cottise-1)
                                                      line-length percentage-base)
                            (cottising/cottise-height (c/++ context :cottising :cottise-2)
                                                      line-length percentage-base))
         :opposite-cottise-height (+ (cottising/cottise-height (c/++ context :cottising :cottise-opposite-1)
                                                               line-length percentage-base)
                                     (cottising/cottise-height (c/++ context :cottising :cottise-opposite-2)
                                                               line-length percentage-base))))
