CREATE TABLE `course` (
  `num_course` bigint NOT NULL AUTO_INCREMENT,
  `level` int NOT NULL,
  `price` float DEFAULT NULL,
  `support` varchar(255) DEFAULT NULL,
  `time_slot` int NOT NULL,
  `type_course` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`num_course`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `instructor` (
  `num_instructor` bigint NOT NULL AUTO_INCREMENT,
  `date_of_hire` date DEFAULT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`num_instructor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `piste` (
  `num_piste` bigint NOT NULL AUTO_INCREMENT,
  `color` varchar(255) DEFAULT NULL,
  `length` int NOT NULL,
  `name_piste` varchar(255) DEFAULT NULL,
  `slope` int NOT NULL,
  PRIMARY KEY (`num_piste`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `subscription` (
  `num_sub` bigint NOT NULL AUTO_INCREMENT,
  `end_date` date DEFAULT NULL,
  `price` float DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `type_sub` int DEFAULT NULL,
  PRIMARY KEY (`num_sub`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `skier` (
  `num_skier` bigint NOT NULL AUTO_INCREMENT,
  `city` varchar(255) DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `subscription_num_sub` bigint DEFAULT NULL,
  PRIMARY KEY (`num_skier`),
  KEY `FK7hb191w2t05f8d6oal4evu6si` (`subscription_num_sub`),
  CONSTRAINT `FK7hb191w2t05f8d6oal4evu6si` FOREIGN KEY (`subscription_num_sub`) REFERENCES `subscription` (`num_sub`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `excursion` (
  `num_skier` bigint NOT NULL,
  `num_piste` bigint NOT NULL,
  PRIMARY KEY (`num_skier`,`num_piste`),
  KEY `FK1ks6qe6k198niwfpqhtw665tr` (`num_piste`),
  CONSTRAINT `FK1ks6qe6k198niwfpqhtw665tr` FOREIGN KEY (`num_piste`) REFERENCES `piste` (`num_piste`),
  CONSTRAINT `FK40mnj3abch3yalf9hqhspospw` FOREIGN KEY (`num_skier`) REFERENCES `skier` (`num_skier`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `instructor_availability` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `day_of_week` varchar(255) DEFAULT NULL,
  `end_time` time DEFAULT NULL,
  `start_time` time DEFAULT NULL,
  `valid_from` date DEFAULT NULL,
  `valid_until` date DEFAULT NULL,
  `instructor_num_instructor` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKs23ktfr24xjs42e4h5tf9upc0` (`instructor_num_instructor`),
  CONSTRAINT `FKs23ktfr24xjs42e4h5tf9upc0` FOREIGN KEY (`instructor_num_instructor`) REFERENCES `instructor` (`num_instructor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `instructor_courses` (
  `instructor_num_instructor` bigint NOT NULL,
  `courses_num_course` bigint NOT NULL,
  PRIMARY KEY (`instructor_num_instructor`,`courses_num_course`),
  UNIQUE KEY `UK_j9pxigt2t61f0arpnppei5txh` (`courses_num_course`),
  CONSTRAINT `FKhrfcfgp8fogkoi1h9k3umjrsw` FOREIGN KEY (`instructor_num_instructor`) REFERENCES `instructor` (`num_instructor`),
  CONSTRAINT `FKqad9j0rkf3c8wbds1e0xrk4ch` FOREIGN KEY (`courses_num_course`) REFERENCES `course` (`num_course`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `payment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(12,2) NOT NULL,
  `method` varchar(255) DEFAULT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `payment_date` datetime(6) DEFAULT NULL,
  `receipt_number` varchar(255) NOT NULL,
  `refunded_amount` decimal(12,2) NOT NULL,
  `status` varchar(255) DEFAULT NULL,
  `transaction_reference` varchar(255) DEFAULT NULL,
  `skier_num_skier` bigint NOT NULL,
  `subscription_num_sub` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_n5hr1jqorqqey5mp18jkfyf84` (`receipt_number`),
  UNIQUE KEY `UK_mkvhnepfwffiubvvearkxb6po` (`transaction_reference`),
  KEY `FKk7f88k52ew7v5ko4rrhtxuusd` (`skier_num_skier`),
  KEY `FK5uhw4vlr0w95hhdpf5fhspw4d` (`subscription_num_sub`),
  CONSTRAINT `FK5uhw4vlr0w95hhdpf5fhspw4d` FOREIGN KEY (`subscription_num_sub`) REFERENCES `subscription` (`num_sub`),
  CONSTRAINT `FKk7f88k52ew7v5ko4rrhtxuusd` FOREIGN KEY (`skier_num_skier`) REFERENCES `skier` (`num_skier`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `refund` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(12,2) NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `refund_date` datetime(6) DEFAULT NULL,
  `payment_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKeoh1147brjy6m009cswl5lty4` (`payment_id`),
  CONSTRAINT `FKeoh1147brjy6m009cswl5lty4` FOREIGN KEY (`payment_id`) REFERENCES `payment` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `planning_slot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `end_time` datetime(6) DEFAULT NULL,
  `max_capacity` int NOT NULL,
  `start_time` datetime(6) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `course_num_course` bigint NOT NULL,
  `instructor_num_instructor` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKl6vfge0ogcd2rxamry2jhvhbg` (`course_num_course`),
  KEY `FKcy28b2ly4f0daj89315fpydta` (`instructor_num_instructor`),
  CONSTRAINT `FKcy28b2ly4f0daj89315fpydta` FOREIGN KEY (`instructor_num_instructor`) REFERENCES `instructor` (`num_instructor`),
  CONSTRAINT `FKl6vfge0ogcd2rxamry2jhvhbg` FOREIGN KEY (`course_num_course`) REFERENCES `course` (`num_course`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `registration` (
  `num_registration` bigint NOT NULL AUTO_INCREMENT,
  `cancelled` tinyint(1) NOT NULL DEFAULT 0,
  `num_week` int NOT NULL,
  `course_num_course` bigint DEFAULT NULL,
  `planning_slot_id` bigint DEFAULT NULL,
  `skier_num_skier` bigint DEFAULT NULL,
  PRIMARY KEY (`num_registration`),
  KEY `FKp67mgguwsa3hm3hg3g5t289kp` (`course_num_course`),
  KEY `FKsgewkdaip194f21klafylhr3r` (`planning_slot_id`),
  KEY `FKjhlqftmyn8buo05bpun5hybmw` (`skier_num_skier`),
  CONSTRAINT `FKjhlqftmyn8buo05bpun5hybmw` FOREIGN KEY (`skier_num_skier`) REFERENCES `skier` (`num_skier`),
  CONSTRAINT `FKp67mgguwsa3hm3hg3g5t289kp` FOREIGN KEY (`course_num_course`) REFERENCES `course` (`num_course`),
  CONSTRAINT `FKsgewkdaip194f21klafylhr3r` FOREIGN KEY (`planning_slot_id`) REFERENCES `planning_slot` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;